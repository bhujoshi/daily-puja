package community

import (
	"net/http"
	"time"

	"github.com/worship/nityamandir/backend/internal/googleauth"
	"github.com/worship/nityamandir/backend/internal/otp"
)

// Caller holds the account lock. Reuse bounded rate limits for Google endpoints.
func (s *Server) allowGoogle(w http.ResponseWriter, r *http.Request) bool {
	if s.Google == nil {
		fail(w, 503, "Google login is not configured yet.")
		return false
	}
	if s.googleLimit == nil {
		s.googleLimit = otp.New(nil)
	}
	if s.googleLimit.Allow(r.RemoteAddr, r.RemoteAddr, false) != nil {
		w.Header().Set("Retry-After", "60")
		fail(w, 429, "Too many sign-in attempts. Please try again later.")
		return false
	}
	return true
}
func (s *Server) googleChallenge(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if !s.allowGoogle(w, r) {
		return
	}
	if s.googleNonces == nil {
		s.googleNonces = map[string]time.Time{}
	}
	for k, expires := range s.googleNonces {
		if !expires.After(s.now()) {
			delete(s.googleNonces, k)
		}
	}
	if len(s.googleNonces) >= 10000 {
		fail(w, 503, "Please retry sign-in later.")
		return
	}
	nonce := random()
	s.googleNonces[nonce] = s.now().Add(5 * time.Minute)
	reply(w, 200, map[string]any{"nonce": nonce, "expires_in_seconds": 300})
}
func (s *Server) verifyGoogle(w http.ResponseWriter, r *http.Request, token, nonce string) (googleauth.Identity, bool) {
	s.mu.Lock()
	if !s.allowGoogle(w, r) {
		s.mu.Unlock()
		return googleauth.Identity{}, false
	}
	expires, ok := s.googleNonces[nonce]
	s.mu.Unlock()
	if !ok || !expires.After(s.now()) {
		fail(w, 401, "Sign-in expired. Please try again.")
		return googleauth.Identity{}, false
	}
	// Network validation does not hold the account lock.
	identity, err := s.Google.Verify(r.Context(), token)
	if err != nil || identity.Subject == "" || identity.Nonce != nonce {
		fail(w, 401, googleauth.ErrInvalid.Error())
		return googleauth.Identity{}, false
	}
	s.mu.Lock()
	expires, ok = s.googleNonces[nonce]
	if ok {
		delete(s.googleNonces, nonce)
	}
	s.mu.Unlock()
	if !ok || !expires.After(s.now()) {
		fail(w, 401, "Sign-in expired. Please try again.")
		return googleauth.Identity{}, false
	}
	return identity, true
}
