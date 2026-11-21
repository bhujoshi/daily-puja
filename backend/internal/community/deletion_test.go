package community

import (
	"encoding/json"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestAccountDeletionRequestFlow(t *testing.T) {
	path := filepath.Join(t.TempDir(), "accounts.json")
	s, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	now := time.Date(2026, 11, 17, 18, 0, 0, 0, time.UTC)
	s.now = func() time.Time { return now }

	// CORS preflight from Firebase-hosted web domain
	preflight := httptest.NewRequest("OPTIONS", "/api/v2/account-deletion-request", nil)
	preflight.Header.Set("Origin", "https://pavitramandir.app")
	pw := httptest.NewRecorder()
	s.ServeHTTP(pw, preflight)
	if pw.Code != 204 || pw.Header().Get("Access-Control-Allow-Origin") != "https://pavitramandir.app" {
		t.Fatalf("unexpected preflight response: %d origin=%q", pw.Code, pw.Header().Get("Access-Control-Allow-Origin"))
	}

	// Validation errors
	if c, _ := call(s, "POST", "account-deletion-request", "", `{"email":"not-an-email"}`); c != 400 {
		t.Fatalf("expected 400 for invalid email, got %d", c)
	}
	tooLong := strings.Repeat("a", 501)
	if c, _ := call(s, "POST", "account-deletion-request", "", `{"email":"user@example.com","reason":"`+tooLong+`"}`); c != 400 {
		t.Fatalf("expected 400 for oversized reason, got %d", c)
	}

	// Create a real account
	c, reg := call(s, "POST", "register", "", `{"email":"bhakt@example.com","password":"long-password"}`)
	if c != 200 {
		t.Fatal(c, reg)
	}
	token := reg["token"].(string)
	accountID := reg["profile"].(map[string]any)["id"].(string)

	// Submit deletion request for the registered email
	c, res := call(s, "POST", "account-deletion-request", "", `{"email":" BHAKT@Example.com ","reason":"Switching devices"}`)
	if c != 202 || res["requested"] != true || res["status"] != "pending" {
		t.Fatalf("unexpected deletion request response: %d %v", c, res)
	}
	refID, _ := res["reference_id"].(string)
	if !strings.HasPrefix(refID, "del_") {
		t.Fatalf("unexpected reference_id: %q", refID)
	}

	// Idempotent for repeat submission of same pending email
	c, repeat := call(s, "POST", "account-deletion-request", "", `{"email":"bhakt@example.com"}`)
	if c != 202 || repeat["reference_id"] != refID {
		t.Fatalf("expected idempotent reference_id %q, got %d %v", refID, c, repeat)
	}

	// Uniform response for unknown email (no account enumeration)
	c, unknown := call(s, "POST", "account-deletion-request", "", `{"email":"unknown@example.com"}`)
	if c != 202 || unknown["requested"] != true {
		t.Fatalf("unexpected response for unknown email: %d %v", c, unknown)
	}

	// Reload from disk and verify persistence & linked account ID
	restored, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	saved := restored.db.DeletionRequests[refID]
	if saved == nil || saved.AccountID != accountID || saved.Email != "bhakt@example.com" || saved.Reason != "Switching devices" {
		b, _ := json.Marshal(saved)
		t.Fatalf("unexpected persisted request: %s", b)
	}

	// In-app immediate deletion cleans up matching pending deletion requests
	if c, _ := call(s, "DELETE", "me", token, ""); c != 200 {
		t.Fatalf("expected 200 on DELETE /me, got %d", c)
	}
	if _, stillExists := s.db.DeletionRequests[refID]; stillExists {
		t.Fatal("expected pending deletion request to be cleared after DELETE /me")
	}

	// Embedded HTML page served cleanly
	pageReq := httptest.NewRequest("GET", "/delete-account", nil)
	pageRec := httptest.NewRecorder()
	DeletionPageHandler().ServeHTTP(pageRec, pageReq)
	if pageRec.Code != 200 || !strings.Contains(pageRec.Body.String(), "/api/v2/account-deletion-request") {
		t.Fatalf("unexpected deletion page response: %d", pageRec.Code)
	}
}
