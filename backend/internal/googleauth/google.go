// Package googleauth validates Google identity before creating local sessions.
package googleauth

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"time"

	"google.golang.org/api/idtoken"
)

var ErrInvalid = errors.New("Google sign-in could not be verified. Please sign in again.")

type Identity struct{ Subject, Email, Nonce string }
type Verifier interface {
	Verify(context.Context, string) (Identity, error)
}
type Service struct {
	audience string
	validate func(context.Context, string, string) (*idtoken.Payload, error)
}

func New(clientID string) (*Service, error) {
	clientID = strings.TrimSpace(clientID)
	if clientID == "" || !strings.HasSuffix(clientID, ".apps.googleusercontent.com") {
		return nil, errors.New("GOOGLE_CLIENT_ID must be a Google Web OAuth client ID")
	}
	validator, err := idtoken.NewValidator(context.Background(), idtoken.WithHTTPClient(&http.Client{Timeout: 8 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}))
	if err != nil {
		return nil, err
	}
	return &Service{audience: clientID, validate: validator.Validate}, nil
}
func (s *Service) Verify(ctx context.Context, token string) (Identity, error) {
	if len(token) == 0 || len(token) > 7000 {
		return Identity{}, ErrInvalid
	}
	p, err := s.validate(ctx, token, s.audience)
	if err != nil || p == nil {
		return Identity{}, ErrInvalid
	}
	if (p.Issuer != "https://accounts.google.com" && p.Issuer != "accounts.google.com") || p.Audience != s.audience || p.Subject == "" || len(p.Subject) > 255 || p.Expires <= time.Now().Unix() {
		return Identity{}, ErrInvalid
	}
	email, _ := p.Claims["email"].(string)
	verified, _ := p.Claims["email_verified"].(bool)
	nonce, _ := p.Claims["nonce"].(string)
	if !verified || email == "" || nonce == "" {
		return Identity{}, ErrInvalid
	}
	return Identity{Subject: p.Subject, Email: email, Nonce: nonce}, nil
}
