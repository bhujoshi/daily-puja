package community

import (
	"context"
	"path/filepath"
	"testing"

	"github.com/worship/nityamandir/backend/internal/otp"
)

type fakeOTP struct {
	err   error
	phone string
	calls int
}

func (f *fakeOTP) Request(_ context.Context, p string) error   { f.phone = p; f.calls++; return f.err }
func (f *fakeOTP) Verify(_ context.Context, p, c string) error { f.phone = p; f.calls++; return f.err }
func TestLiveOTPAccount(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	provider := &fakeOTP{}
	s.OTP = otp.New(provider)
	if c, _ := call(s, "POST", "auth/otp/request", "", `{"phone":"+919876543210"}`); c != 200 {
		t.Fatal(c)
	}
	if provider.phone != "+919876543210" {
		t.Fatal(provider.phone)
	}
	if c, _ := call(s, "POST", "auth/otp/request", "", `{"phone":"9876543210"}`); c != 429 {
		t.Fatal(c)
	}
	provider.err = otp.ErrInvalid
	if c, _ := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"123456"}`); c != 401 || len(s.db.Accounts) != 0 {
		t.Fatal("invalid code created account", c)
	}
	provider.err = otp.ErrUnavailable
	if c, _ := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"123456"}`); c != 503 {
		t.Fatal(c)
	}
	provider.err = nil
	c, result := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"123456"}`)
	if c != 200 {
		t.Fatal(c, result)
	}
	token := result["token"].(string)
	restored, _ := New(s.path)
	if c, _ := call(restored, "GET", "me", token, ""); c != 200 {
		t.Fatal("session not persisted", c)
	}
	if c, _ := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"1x3456"}`); c != 400 {
		t.Fatal(c)
	}
}
