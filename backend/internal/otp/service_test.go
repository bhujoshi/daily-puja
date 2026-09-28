package otp

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestTwilioWhatsApp(t *testing.T) {
	status, body := 200, `{"status":"pending"}`
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		user, pass, ok := r.BasicAuth()
		if !ok || user != "account" || pass != "secret" {
			t.Error("missing credentials")
		}
		if err := r.ParseForm(); err != nil {
			t.Fatal(err)
		}
		if r.Form.Get("To") != "+919876543210" {
			t.Error("wrong phone")
		}
		if r.URL.Path == "/service/Verifications" && r.Form.Get("Channel") != "whatsapp" {
			t.Error("wrong channel")
		}
		if r.URL.Path == "/service/VerificationCheck" && r.Form.Get("Code") != "123456" {
			t.Error("wrong code")
		}
		w.WriteHeader(status)
		_, _ = w.Write([]byte(body))
	})
	client := &http.Client{Transport: roundTripFunc(func(r *http.Request) (*http.Response, error) {
		w := httptest.NewRecorder()
		handler.ServeHTTP(w, r)
		return w.Result(), nil
	})}
	p := &twilio{account: "account", token: "secret", service: "service", channel: "whatsapp", base: "https://verify.example/", client: client}
	if err := p.Request(context.Background(), "+919876543210"); err != nil {
		t.Fatal(err)
	}
	if err := p.Verify(context.Background(), "+919876543210", "123456"); err != ErrInvalid {
		t.Fatal("pending accepted", err)
	}
	body = `{"status":"approved"}`
	if err := p.Verify(context.Background(), "+919876543210", "123456"); err != nil {
		t.Fatal(err)
	}
	for _, c := range []int{404, 429, 500} {
		status = c
		err := p.Verify(context.Background(), "+919876543210", "123456")
		expected := ErrUnavailable
		if c == 404 {
			expected = ErrInvalid
		}
		if c == 429 {
			expected = ErrLimited
		}
		if err != expected {
			t.Fatal(c, err)
		}
	}
	status = 200
	body = `broken`
	if err := p.Verify(context.Background(), "+919876543210", "123456"); err != ErrUnavailable {
		t.Fatal(err)
	}
}

func TestLimits(t *testing.T) {
	s := New(nil)
	now := time.Now()
	s.now = func() time.Time { return now }
	for i := 0; i < 5; i++ {
		if err := s.Allow("9876543210", "127.0.0.1:12", true); err != nil {
			t.Fatal(i, err)
		}
		if err := s.Allow("9876543210", "127.0.0.1:13", true); err != ErrLimited {
			t.Fatal("cooldown missing")
		}
		now = now.Add(time.Minute)
	}
	if err := s.Allow("9876543210", "127.0.0.1:12", true); err != ErrLimited {
		t.Fatal("hour limit missing")
	}
	now = now.Add(time.Hour)
	if err := s.Allow("9876543210", "127.0.0.1:12", true); err != nil {
		t.Fatal("limit never resets")
	}
	for i := 0; i < 20; i++ {
		if err := s.Allow("9876543210", "127.0.0.1:12", false); err != nil {
			t.Fatal(err)
		}
	}
	if err := s.Allow("9876543210", "127.0.0.1:12", false); err != ErrLimited {
		t.Fatal("verification limit missing")
	}
}

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }
