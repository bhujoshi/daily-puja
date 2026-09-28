package otp

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"net/http"
	"strings"
)

// Delivery channel and fallback are selected in the Fast2SMS OTP ID configuration.
type fast2sms struct {
	key, otpID, base string
	client           *http.Client
}

func (f *fast2sms) post(ctx context.Context, endpoint string, body any, checking bool) error {
	data, err := json.Marshal(body)
	if err != nil {
		return ErrUnavailable
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, f.base+endpoint, bytes.NewReader(data))
	if err != nil {
		return ErrUnavailable
	}
	req.Header.Set("Authorization", f.key)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	resp, err := f.client.Do(req)
	if err != nil {
		return ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode == 429 {
		return ErrLimited
	}
	if checking && (resp.StatusCode == 400 || resp.StatusCode == 404) {
		return ErrInvalid
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return ErrUnavailable
	}
	var result struct {
		Success bool `json:"return"`
		Status  int  `json:"status_code"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 16384)).Decode(&result) != nil {
		return ErrUnavailable
	}
	if result.Status == 429 {
		return ErrLimited
	}
	if checking && (result.Status == 400 || result.Status == 404) {
		return ErrInvalid
	}
	// Never issue a session on HTTP success alone or malformed provider responses.
	if !result.Success || result.Status != 200 {
		return ErrUnavailable
	}
	return nil
}
func (f *fast2sms) Request(ctx context.Context, phone string) error {
	return f.post(ctx, "send", map[string]any{"mobile": strings.TrimPrefix(phone, "+91"), "otp_id": f.otpID, "otp_length": 6, "otp_expiry": 5}, false)
}
func (f *fast2sms) Verify(ctx context.Context, phone, code string) error {
	return f.post(ctx, "verify", map[string]string{"mobile": strings.TrimPrefix(phone, "+91"), "otp": code}, true)
}
