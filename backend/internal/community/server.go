// Package community implements the account-backed customization pilot.
// The atomic file store supports one server process; migrate to SQL before scaling.
package community

import (
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"net/mail"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

type Account struct {
	Phone      string            `json:"phone"`
	History    []Event           `json:"history"`
	ID         string            `json:"id"`
	Email      string            `json:"email"`
	Salt       string            `json:"salt"`
	Hash       string            `json:"hash"`
	Invite     string            `json:"invite_code"`
	Referrer   string            `json:"referrer"`
	Days       []string          `json:"days"`
	Selections map[string]string `json:"selections"`
	Unlocked   bool              `json:"unlocked"`
	Referrals  int               `json:"referrals"`
}
type Event struct {
	ID     string    `json:"id"`
	Kind   string    `json:"kind"`
	At     time.Time `json:"at"`
	Detail string    `json:"detail"`
}

func (s *Server) event(a *Account, kind, detail string) {
	a.History = append(a.History, Event{random(), kind, s.now(), detail})
}

type Session struct {
	User    string
	Expires time.Time
}
type database struct {
	Accounts map[string]*Account
	Sessions map[string]Session
}
type Server struct {
	MockMode bool
	mu       sync.Mutex
	path     string
	db       database
	now      func() time.Time
}

var india = time.FixedZone("Asia/Kolkata", 19800)

func New(path string) (*Server, error) {
	s := &Server{path: path, now: time.Now, db: database{map[string]*Account{}, map[string]Session{}}}
	b, err := os.ReadFile(path)
	if err == nil {
		err = json.Unmarshal(b, &s.db)
	}
	if err != nil && !errors.Is(err, os.ErrNotExist) {
		return nil, err
	}
	return s, nil
}
func random() string {
	b := make([]byte, 24)
	if _, err := rand.Read(b); err != nil {
		panic(err)
	}
	return hex.EncodeToString(b)
}
func digest(v string) string { h := sha256.Sum256([]byte(v)); return hex.EncodeToString(h[:]) }
func password(v, salt string) string {
	b, _ := pbkdf2.Key(sha256.New, v, []byte(salt), 600000, 32)
	return hex.EncodeToString(b)
}
func (s *Server) save() error {
	if err := os.MkdirAll(filepath.Dir(s.path), 0700); err != nil {
		return err
	}
	b, err := json.Marshal(s.db)
	if err != nil {
		return err
	}
	f, err := os.CreateTemp(filepath.Dir(s.path), ".accounts-*")
	if err != nil {
		return err
	}
	defer os.Remove(f.Name())
	if _, err = f.Write(b); err == nil {
		err = f.Sync()
	}
	closeErr := f.Close()
	if err != nil {
		return err
	}
	if closeErr != nil {
		return closeErr
	}
	return os.Rename(f.Name(), s.path)
}
func reply(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
func fail(w http.ResponseWriter, status int, msg string) {
	reply(w, status, map[string]string{"error": msg})
}
func streak(a *Account, now time.Time) int {
	day := now.In(india)
	seen := map[string]bool{}
	for _, d := range a.Days {
		seen[d] = true
	}
	if !seen[day.Format("2006-01-02")] {
		day = day.AddDate(0, 0, -1)
	}
	n := 0
	for seen[day.Format("2006-01-02")] {
		n++
		day = day.AddDate(0, 0, -1)
	}
	return n
}
func (s *Server) profile(a *Account) any {
	return map[string]any{"id": a.ID, "phone": a.Phone, "history": a.History, "mock_payments": s.MockMode, "email": a.Email, "invite_code": a.Invite, "days": a.Days, "streak": streak(a, s.now()), "unlocked": a.Unlocked, "referrals": a.Referrals, "selections": a.Selections, "streak_target": 7, "referral_target": 1}
}
func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()
	// Roll back memory as well as disk on persistence failure.
	before, _ := json.Marshal(s.db)
	commit := func() bool {
		if err := s.save(); err != nil {
			s.db = database{}
			_ = json.Unmarshal(before, &s.db)
			fail(w, 503, "Could not save. Please retry.")
			return false
		}
		return true
	}
	path := strings.TrimPrefix(r.URL.Path, "/api/v2/")
	var body struct {
		Phone      string            `json:"phone"`
		OTP        string            `json:"otp"`
		RequestID  string            `json:"request_id"`
		Email      string            `json:"email"`
		Password   string            `json:"password"`
		Invite     string            `json:"invite_code"`
		Steps      []string          `json:"steps"`
		Selections map[string]string `json:"selections"`
	}
	if r.Method == "POST" || r.Method == "PUT" {
		dec := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8192))
		dec.DisallowUnknownFields()
		if err := dec.Decode(&body); err != nil {
			fail(w, 400, "Invalid request")
			return
		}
		if dec.Decode(&struct{}{}) != io.EOF {
			fail(w, 400, "Invalid request")
			return
		}
	}
	if path == "catalog" && r.Method == "GET" {
		reply(w, 200, json.RawMessage(catalogJSON))
		return
	}
	if (path == "auth/otp/request" || path == "auth/otp/verify") && r.Method == "POST" {
		if !s.MockMode {
			fail(w, 503, "Mobile login is not configured. Demo mode is disabled.")
			return
		}
		phone := strings.TrimSpace(body.Phone)
		phone = strings.TrimPrefix(phone, "+91")
		if len(phone) != 10 || phone[0] < '6' || phone[0] > '9' || strings.Trim(phone, "0123456789") != "" {
			fail(w, 400, "Enter a valid 10-digit Indian mobile number")
			return
		}
		if path == "auth/otp/request" {
			reply(w, 200, map[string]any{"mock": true, "message": "Demo only: use OTP 1234. No SMS sent."})
			return
		}
		if body.OTP != "1234" {
			fail(w, 401, "Incorrect demo OTP. Use 1234.")
			return
		}
		key := "phone:" + phone
		a := s.db.Accounts[key]
		if a == nil {
			ref := ""
			if body.Invite != "" {
				for _, other := range s.db.Accounts {
					if other.Invite == body.Invite {
						ref = other.ID
					}
				}
				if ref == "" {
					fail(w, 400, "Invitation code not found")
					return
				}
			}
			a = &Account{ID: random(), Phone: phone, Invite: random()[:12], Referrer: ref, Days: []string{}, Selections: map[string]string{}}
			s.db.Accounts[key] = a
			s.event(a, "account_created", "Mobile demo account created")
		}
		token := random()
		s.db.Sessions[digest(token)] = Session{key, s.now().Add(30 * 24 * time.Hour)}
		if commit() {
			reply(w, 200, map[string]any{"token": token, "profile": s.profile(a)})
		}
		return
	}
	if (path == "register" || path == "login") && r.Method == "POST" {
		body.Email = strings.ToLower(strings.TrimSpace(body.Email))
		addr, err := mail.ParseAddress(body.Email)
		if err != nil || addr.Address != body.Email || len(body.Email) > 254 || len(body.Password) < 10 || len(body.Password) > 128 {
			fail(w, 400, "Enter a valid email and a password of 10–128 characters")
			return
		}
		a := s.db.Accounts[body.Email]
		if path == "register" {
			if a != nil {
				fail(w, 409, "Account already exists. Sign in instead.")
				return
			}
			ref := ""
			if body.Invite != "" {
				for _, other := range s.db.Accounts {
					if other.Invite == body.Invite {
						ref = other.ID
					}
				}
				if ref == "" {
					fail(w, 400, "Invitation code not found")
					return
				}
			}
			a = &Account{ID: random(), Email: body.Email, Salt: random(), Invite: random()[:12], Referrer: ref, Days: []string{}, Selections: map[string]string{}}
			a.Hash = password(body.Password, a.Salt)
			s.db.Accounts[body.Email] = a
		} else {
			salt := "unknown-account"
			if a != nil {
				salt = a.Salt
			}
			hash := password(body.Password, salt)
			if a == nil || subtle.ConstantTimeCompare([]byte(hash), []byte(a.Hash)) != 1 {
				fail(w, 401, "Email or password is incorrect")
				return
			}
		}
		token := random()
		for k, v := range s.db.Sessions {
			if v.Expires.Before(s.now()) {
				delete(s.db.Sessions, k)
			}
		}
		s.db.Sessions[digest(token)] = Session{a.Email, s.now().Add(30 * 24 * time.Hour)}
		if commit() {
			reply(w, 200, map[string]any{"token": token, "profile": s.profile(a)})
		}
		return
	}
	token := strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")
	session, ok := s.db.Sessions[digest(token)]
	if !ok || !session.Expires.After(s.now()) {
		fail(w, 401, "Please sign in")
		return
	}
	a := s.db.Accounts[session.User]
	if a == nil {
		fail(w, 401, "Please sign in")
		return
	}
	switch {
	case path == "me" && r.Method == "GET":
		reply(w, 200, s.profile(a))
	case path == "logout" && r.Method == "POST":
		delete(s.db.Sessions, digest(token))
		if commit() {
			reply(w, 200, map[string]bool{"ok": true})
		}
	case path == "me" && r.Method == "DELETE":
		delete(s.db.Accounts, session.User)
		for k, v := range s.db.Sessions {
			if v.User == session.User {
				delete(s.db.Sessions, k)
			}
		}
		if commit() {
			reply(w, 200, map[string]bool{"deleted": true})
		}
	case path == "activity/puja" && r.Method == "POST":
		expected := []string{"LIGHT", "BATH", "TILAK", "FLOWERS", "BELL", "CONCH", "PRASAD", "AARTI"}
		if len(body.Steps) != len(expected) {
			fail(w, 400, "Complete all eight puja steps")
			return
		}
		for i, v := range expected {
			if body.Steps[i] != v {
				fail(w, 400, "Invalid puja steps")
				return
			}
		}
		today := s.now().In(india).Format("2006-01-02")
		for _, d := range a.Days {
			if d == today {
				reply(w, 200, s.profile(a))
				return
			}
		}
		first := len(a.Days) == 0
		a.Days = append(a.Days, today)
		s.event(a, "puja_completed", today)
		if streak(a, s.now()) >= 7 {
			if !a.Unlocked {
				s.event(a, "streak_unlock", "Seven-day puja streak")
			}
			a.Unlocked = true
		}
		if first && a.Referrer != "" {
			for _, other := range s.db.Accounts {
				if other.ID == a.Referrer {
					s.event(other, "referral_unlock", "Invited friend completed their first puja")
					other.Referrals++
					other.Unlocked = true
				}
			}
		}
		if commit() {
			reply(w, 200, s.profile(a))
		}
	case path == "shrine" && r.Method == "PUT":
		if !a.Unlocked {
			fail(w, 403, "Unlock customization with a daily streak or an invitation")
			return
		}
		// Accept the old six-category client by supplying the original oil lamp.
		if _, ok := body.Selections["lamp"]; !ok && len(body.Selections) == 6 {
			body.Selections["lamp"] = "original"
		}
		if !validSelections(body.Selections) {
			fail(w, 400, "Choose an available item in every category")
			return
		}

		a.Selections = body.Selections
		details, _ := json.Marshal(body.Selections)
		s.event(a, "shrine_saved", string(details))
		if commit() {
			reply(w, 200, s.profile(a))
		}
	case path == "purchase" && r.Method == "POST":
		if !s.MockMode {
			fail(w, 503, "Mock payments are disabled. No payment taken.")
			return
		}
		if len(body.RequestID) < 8 || len(body.RequestID) > 128 {
			fail(w, 400, "A payment request ID is required")
			return
		}
		for _, event := range a.History {
			if event.Kind == "mock_purchase" && event.ID == body.RequestID {
				reply(w, 200, s.profile(a))
				return
			}
		}
		if !a.Unlocked {
			a.History = append(a.History, Event{body.RequestID, "mock_purchase", s.now(), "Temple customization package · demo success · INR 0 charged"})
			a.Unlocked = true
		}
		if commit() {
			reply(w, 200, s.profile(a))
		}
	default:
		fail(w, 404, "Endpoint not found")
	}
}
