package otp

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestFast2SMS(t *testing.T) {
	status, body := 200, `{"return":true,"status_code":200}`
	client := &http.Client{Transport: roundTripFunc(func(r *http.Request) (*http.Response, error) {
		if r.Header.Get("Authorization") != "test-key" || r.Header.Get("Content-Type") != "application/json" {
			t.Error("missing headers")
		}
		var payload map[string]any
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Fatal(err)
		}
		if payload["mobile"] != "9876543210" {
			t.Error("phone must be 10 digits")
		}
		switch r.URL.Path {
		case "/send":
			if payload["otp_id"] != "template-1" || payload["otp_length"] != float64(6) || payload["otp_expiry"] != float64(5) {
				t.Error("incorrect send settings")
			}
			if _, ok := payload["otp"]; ok {
				t.Error("backend supplied a code")
			}
		case "/verify":
			if payload["otp"] != "123456" {
				t.Error("incorrect verification code")
			}
		default:
			t.Error("wrong endpoint")
		}
		w := httptest.NewRecorder()
		w.WriteHeader(status)
		_, _ = w.Write([]byte(body))
		return w.Result(), nil
	})}
	p := &fast2sms{key: "test-key", otpID: "template-1", base: "https://example.test/", client: client}
	if err := p.Request(context.Background(), "+919876543210"); err != nil {
		t.Fatal(err)
	}
	if err := p.Verify(context.Background(), "+919876543210", "123456"); err != nil {
		t.Fatal(err)
	}
	for _, tc := range []struct {
		status int
		body   string
		want   error
	}{
		{400, `{}`, ErrInvalid}, {404, `{}`, ErrInvalid}, {401, `{}`, ErrUnavailable}, {429, `{}`, ErrLimited}, {500, `{}`, ErrUnavailable},
		{200, `{"return":false,"status_code":400}`, ErrInvalid}, {200, `{"return":false,"status_code":401}`, ErrUnavailable},
		{200, `{"return":false,"status_code":200}`, ErrUnavailable}, {200, `{"return":true}`, ErrUnavailable},
		{200, `{}`, ErrUnavailable}, {200, `broken`, ErrUnavailable},
	} {
		status, body = tc.status, tc.body
		if err := p.Verify(context.Background(), "+919876543210", "123456"); err != tc.want {
			t.Fatal(tc, err)
		}
	}
	status = 400
	body = `{}`
	if err := p.Request(context.Background(), "+919876543210"); err != ErrUnavailable {
		t.Fatal("configuration failure treated as wrong code", err)
	}
	p.client = &http.Client{Transport: roundTripFunc(func(*http.Request) (*http.Response, error) { return nil, errors.New("network failure") })}
	if err := p.Verify(context.Background(), "+919876543210", "123456"); err != ErrUnavailable {
		t.Fatal(err)
	}
}

func TestFast2SMSEnv(t *testing.T) {
	t.Setenv("OTP_PROVIDER", "fast2sms")
	t.Setenv("FAST2SMS_API_KEY", "")
	t.Setenv("FAST2SMS_OTP_ID", "")
	if _, err := FromEnv(); err == nil {
		t.Fatal("missing credentials accepted")
	}
	t.Setenv("FAST2SMS_API_KEY", "test-key")
	t.Setenv("FAST2SMS_OTP_ID", "template-1")
	service, err := FromEnv()
	if err != nil {
		t.Fatal(err)
	}
	if _, ok := service.provider.(*fast2sms); !ok {
		t.Fatal("wrong provider")
	}
	t.Setenv("OTP_PROVIDER", "unknown")
	if _, err := FromEnv(); err == nil {
		t.Fatal("unknown provider accepted")
	}
}
