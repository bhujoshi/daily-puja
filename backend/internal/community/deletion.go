package community

import (
	_ "embed"
	"net/http"
	"net/mail"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/worship/nityamandir/backend/internal/otp"
)

//go:embed delete_account.html
var deleteAccountHTML []byte

// DeletionRequest records a web-submitted request to delete an account and its associated data.
type DeletionRequest struct {
	ID          string    `json:"id"`
	Email       string    `json:"email"`
	AccountID   string    `json:"account_id,omitempty"`
	Reason      string    `json:"reason,omitempty"`
	Status      string    `json:"status"`
	Source      string    `json:"source"`
	RequestedAt time.Time `json:"requested_at"`
}

var allowedDeletionOrigins = map[string]bool{
	"https://pavitramandir.app":            true,
	"https://www.pavitramandir.app":        true,
	"https://pavitra-mandir.web.app":       true,
	"https://pavitra-mandir.firebaseapp.com": true,
	"http://localhost:5000":                true,
	"http://127.0.0.1:5000":                true,
}

func setDeletionCORS(w http.ResponseWriter, r *http.Request) {
	w.Header().Add("Vary", "Origin")
	origin := r.Header.Get("Origin")
	if allowedDeletionOrigins[origin] {
		w.Header().Set("Access-Control-Allow-Origin", origin)
		w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
	}
}

// DeletionPageHandler serves a standalone HTML account-deletion request page from the API host.
func DeletionPageHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet && r.Method != http.MethodHead {
			fail(w, http.StatusMethodNotAllowed, "Method not allowed")
			return
		}
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		w.Header().Set("Cache-Control", "public, max-age=300")
		w.WriteHeader(http.StatusOK)
		if r.Method == http.MethodGet {
			_, _ = w.Write(deleteAccountHTML)
		}
	})
}

func (s *Server) handleAccountDeletionRequest(w http.ResponseWriter, r *http.Request, body rewardRequest, commit func() bool) {
	email := strings.ToLower(strings.TrimSpace(body.Email))
	addr, err := mail.ParseAddress(email)
	if err != nil || addr.Address != email || len(email) > 254 {
		fail(w, http.StatusBadRequest, "Enter a valid email address")
		return
	}
	reason := strings.TrimSpace(body.Reason)
	if utf8.RuneCountInString(reason) > 500 {
		fail(w, http.StatusBadRequest, "Reason must be 500 characters or fewer")
		return
	}
	if s.deletionLimit == nil {
		s.deletionLimit = otp.New(nil)
	}
	if s.deletionLimit.Allow(digest("deletion:"+email), r.RemoteAddr, false) != nil {
		w.Header().Set("Retry-After", "60")
		fail(w, http.StatusTooManyRequests, "Too many deletion requests. Please try again later.")
		return
	}

	if s.db.DeletionRequests == nil {
		s.db.DeletionRequests = map[string]*DeletionRequest{}
	}
	for _, existing := range s.db.DeletionRequests {
		if existing.Status == "pending" && strings.EqualFold(existing.Email, email) {
			reply(w, http.StatusAccepted, deletionRequestResponse(existing.ID))
			return
		}
	}

	accountID := ""
	for _, acc := range s.db.Accounts {
		if strings.EqualFold(acc.Email, email) {
			accountID = acc.ID
			break
		}
	}

	req := &DeletionRequest{
		ID:          "del_" + random()[:16],
		Email:       email,
		AccountID:   accountID,
		Reason:      reason,
		Status:      "pending",
		Source:      "web",
		RequestedAt: s.now(),
	}
	s.db.DeletionRequests[req.ID] = req
	if commit() {
		reply(w, http.StatusAccepted, deletionRequestResponse(req.ID))
	}
}

func deletionRequestResponse(referenceID string) map[string]any {
	return map[string]any{
		"requested":    true,
		"reference_id": referenceID,
		"status":       "pending",
		"message":      "Your account deletion request has been recorded. Any matching Pavitra Mandir account and associated data will be deleted within 7 days.",
	}
}
