// Package otp delegates code generation, expiry and single-use verification to configured verification providers.
package otp

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"strings"
	"sync"
	"time"
)

var (
	ErrInvalid     = errors.New("Invalid or expired OTP. Request a new code.")
	ErrLimited     = errors.New("Too many OTP attempts. Please try again later.")
	ErrUnavailable = errors.New("Phone verification is unavailable. Please retry later.")
)

type Provider interface {
	Request(context.Context, string) error
	Verify(context.Context, string, string) error
}

type bucket struct {
	start time.Time
	count int
	last  time.Time
}
type Service struct {
	provider Provider
	mu       sync.Mutex
	buckets  map[string]bucket
	now      func() time.Time
}

func New(provider Provider) *Service {
	return &Service{provider: provider, buckets: map[string]bucket{}, now: time.Now}
}

// Limits are per process. RemoteAddr is used rather than untrusted forwarding headers.
func (s *Service) Allow(phone, remote string, sending bool) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	now := s.now()
	for k, b := range s.buckets {
		if now.Sub(b.start) >= time.Hour {
			delete(s.buckets, k)
		}
	}
	ip, _, err := net.SplitHostPort(remote)
	if err != nil {
		ip = remote
	}
	prefix := "verify:"
	phoneMax, ipMax := 20, 100
	if sending {
		prefix = "send:"
		phoneMax, ipMax = 5, 30
	}
	keys := []string{prefix + "phone:" + phone, prefix + "ip:" + ip}
	for i, k := range keys {
		b := s.buckets[k]
		limit := phoneMax
		if i == 1 {
			limit = ipMax
		}
		if b.count >= limit || (sending && i == 0 && b.count > 0 && now.Sub(b.last) < time.Minute) {
			return ErrLimited
		}
	}
	// Bound memory even under distributed requests with unique destinations.
	if len(s.buckets)+2 > 10000 {
		return ErrLimited
	}
	for _, k := range keys {
		b := s.buckets[k]
		if b.count == 0 {
			b.start = now
		}
		b.count++
		b.last = now
		s.buckets[k] = b
	}
	return nil
}
func (s *Service) Request(ctx context.Context, phone string) error {
	return s.provider.Request(ctx, phone)
}
func (s *Service) Verify(ctx context.Context, phone, code string) error {
	return s.provider.Verify(ctx, phone, code)
}

var sidPattern = regexp.MustCompile(`^(AC|VA)[0-9a-fA-F]{32}$`)

// FromEnv fails startup for incomplete credentials; an unset provider disables live OTP.
func FromEnv() (*Service, error) {
	provider := os.Getenv("OTP_PROVIDER")
	if provider == "" {
		return nil, nil
	}
	if provider == "fast2sms" {
		key, id := strings.TrimSpace(os.Getenv("FAST2SMS_API_KEY")), strings.TrimSpace(os.Getenv("FAST2SMS_OTP_ID"))
		if key == "" || id == "" {
			return nil, errors.New("FAST2SMS_API_KEY and FAST2SMS_OTP_ID are required")
		}
		return New(&fast2sms{key: key, otpID: id, base: "https://www.fast2sms.com/dev/otp/", client: providerClient()}), nil
	}
	if provider != "twilio" {
		return nil, errors.New("OTP_PROVIDER must be fast2sms or twilio")
	}
	channel := os.Getenv("OTP_CHANNEL")
	if channel == "" {
		channel = "whatsapp"
	}
	if channel != "whatsapp" && channel != "sms" {
		return nil, errors.New("OTP_CHANNEL must be whatsapp or sms")
	}
	account, token, service := os.Getenv("TWILIO_ACCOUNT_SID"), os.Getenv("TWILIO_AUTH_TOKEN"), os.Getenv("TWILIO_VERIFY_SERVICE_SID")
	if !sidPattern.MatchString(account) || !strings.HasPrefix(account, "AC") || !sidPattern.MatchString(service) || !strings.HasPrefix(service, "VA") || strings.TrimSpace(token) == "" {
		return nil, errors.New("valid TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_VERIFY_SERVICE_SID are required")
	}
	return New(&twilio{account: account, token: token, service: service, channel: channel, base: "https://verify.twilio.com/v2/Services/", client: providerClient()}), nil
}

type twilio struct {
	account, token, service, base, channel string
	client                                 *http.Client
}

func (t *twilio) post(ctx context.Context, endpoint string, values url.Values, checking bool) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, t.base+t.service+"/"+endpoint, strings.NewReader(values.Encode()))
	if err != nil {
		return ErrUnavailable
	}
	req.SetBasicAuth(t.account, t.token)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := t.client.Do(req)
	if err != nil {
		return ErrUnavailable
	}
	defer resp.Body.Close()
	if resp.StatusCode == 429 {
		return ErrLimited
	}
	if checking && resp.StatusCode == 404 {
		return ErrInvalid
	}
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return ErrUnavailable
	}
	var result struct {
		Status string `json:"status"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 16384)).Decode(&result) != nil {
		return ErrUnavailable
	}
	if checking {
		if result.Status != "approved" {
			return ErrInvalid
		}
	} else if result.Status != "pending" {
		return ErrUnavailable
	}
	return nil
}
func (t *twilio) Request(ctx context.Context, phone string) error {
	return t.post(ctx, "Verifications", url.Values{"To": {phone}, "Channel": {t.channel}}, false)
}
func (t *twilio) Verify(ctx context.Context, phone, code string) error {
	return t.post(ctx, "VerificationCheck", url.Values{"To": {phone}, "Code": {code}}, true)
}

func providerClient() *http.Client {
	return &http.Client{Timeout: 8 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
}
