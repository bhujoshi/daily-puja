package community

import (
	"encoding/json"
	"net/http/httptest"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func call(s *Server, method, path, token, body string) (int, map[string]any) {
	r := httptest.NewRequest(method, "/api/v2/"+path, strings.NewReader(body))
	r.Header.Set("Authorization", "Bearer "+token)
	w := httptest.NewRecorder()
	s.ServeHTTP(w, r)
	var v map[string]any
	_ = json.Unmarshal(w.Body.Bytes(), &v)
	return w.Code, v
}

const steps = `{"steps":["LIGHT","BATH","TILAK","FLOWERS","BELL","CONCH","PRASAD","AARTI"]}`

func TestAccountJourney(t *testing.T) {
	path := filepath.Join(t.TempDir(), "accounts.json")
	s, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	now := time.Date(2026, 9, 20, 18, 29, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	code, v := call(s, "POST", "register", "", `{"email":"a@example.com","password":"long-password"}`)
	if code != 200 {
		t.Fatal(v)
	}
	token := v["token"].(string)
	if c, _ := call(s, "GET", "me", "fake", ""); c != 401 {
		t.Fatal(c)
	}
	if c, _ := call(s, "PUT", "shrine", token, `{"selections":{}}`); c != 403 {
		t.Fatal(c)
	}
	if c, _ := call(s, "POST", "activity/puja", token, `{"steps":["AARTI"]}`); c != 400 {
		t.Fatal(c)
	}
	for i := 0; i < 7; i++ {
		if i > 0 {
			now = now.Add(24 * time.Hour)
		}
		for repeat := 0; repeat < 2; repeat++ {
			c, p := call(s, "POST", "activity/puja", token, steps)
			if c != 200 || int(p["streak"].(float64)) != i+1 {
				t.Fatal(c, p)
			}
		}
	}
	_, v = call(s, "GET", "me", token, "")
	if !v["unlocked"].(bool) || len(v["days"].([]any)) != 7 {
		t.Fatal(v)
	}
	restored, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	restored.now = s.now
	if c, _ := call(restored, "GET", "me", token, ""); c != 200 {
		t.Fatal(c)
	}
	if c, _ := call(s, "POST", "purchase", token, `{}`); c != 503 {
		t.Fatal(c)
	}
	if c, _ := call(s, "DELETE", "me", token, ""); c != 200 {
		t.Fatal(c)
	}
	if c, _ := call(s, "GET", "me", token, ""); c != 401 {
		t.Fatal(c)
	}
}
func TestReferralAndIndiaMidnight(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	now := time.Date(2026, 9, 20, 18, 29, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	_, v := call(s, "POST", "register", "", `{"email":"a@example.com","password":"long-password"}`)
	owner := v["token"].(string)
	invite := v["profile"].(map[string]any)["invite_code"].(string)
	_, v = call(s, "POST", "register", "", `{"email":"b@example.com","password":"long-password","invite_code":"`+invite+`"}`)
	friend := v["token"].(string)
	_, v = call(s, "GET", "me", owner, "")
	if v["unlocked"].(bool) {
		t.Fatal("signup alone rewarded")
	}
	call(s, "POST", "activity/puja", friend, steps)
	call(s, "POST", "activity/puja", friend, steps)
	_, v = call(s, "GET", "me", owner, "")
	if !v["unlocked"].(bool) || v["referrals"].(float64) != 1 {
		t.Fatal(v)
	}
	now = now.Add(2 * time.Minute)
	_, v = call(s, "POST", "activity/puja", friend, steps)
	if v["streak"].(float64) != 2 {
		t.Fatal(v)
	}
	now = now.Add(48 * time.Hour)
	_, v = call(s, "GET", "me", friend, "")
	if v["streak"].(float64) != 0 {
		t.Fatal(v)
	}
}

func TestFailedSaveRollsBack(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	s.path = t.TempDir() // A directory cannot be replaced by the account file.
	c, _ := call(s, "POST", "register", "", `{"email":"a@example.com","password":"long-password"}`)
	if c != 503 || len(s.db.Accounts) != 0 || len(s.db.Sessions) != 0 {
		t.Fatalf("failed write leaked state: %d", c)
	}
}

func TestCatalogSelectionIsAuthorizedAndPersistent(t *testing.T) {
	path := filepath.Join(t.TempDir(), "accounts.json")
	s, _ := New(path)
	code, catalog := call(s, "GET", "catalog", "", "")
	if code != 200 || !catalog["asset_variants_available"].(bool) {
		t.Fatal(catalog)
	}
	_, registered := call(s, "POST", "register", "", `{"email":"custom@example.com","password":"long-password"}`)
	token := registered["token"].(string)
	body := `{"selections":{"shrine":"marble","idols":"shiva","flowers":"rose","lamp":"brass","shankh":"ivory","aarti":"traditional","prasad":"halwa"}}`
	if c, _ := call(s, "PUT", "shrine", token, body); c != 403 {
		t.Fatal("locked account changed shrine", c)
	}
	s.db.Accounts["custom@example.com"].Unlocked = true
	if c, _ := call(s, "PUT", "shrine", token, strings.Replace(body, `"rose"`, `"../../private"`, 1)); c != 400 {
		t.Fatal("invalid asset accepted", c)
	}
	if c, _ := call(s, "PUT", "shrine", token, body); c != 200 {
		t.Fatal(c)
	}
	restored, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	_, profile := call(restored, "GET", "me", token, "")
	if profile["selections"].(map[string]any)["idols"] != "shiva" {
		t.Fatal(profile)
	}
	if c, _ := call(s, "PUT", "shrine", token, strings.Replace(body, `,"lamp":"brass"`, "", 1)); c != 200 {
		t.Fatal("legacy client failed", c)
	}
}

func TestMockMobilePaymentPersistence(t *testing.T) {
	path := filepath.Join(t.TempDir(), "accounts.json")
	s, _ := New(path)
	if c, _ := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"1234"}`); c != 503 {
		t.Fatal(c)
	}
	s.MockMode = true
	for _, body := range []string{`{"phone":"123","otp":"1234"}`, `{"phone":"987654321x","otp":"1234"}`} {
		if c, _ := call(s, "POST", "auth/otp/verify", "", body); c != 400 {
			t.Fatal(c)
		}
	}
	if c, _ := call(s, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"0000"}`); c != 401 {
		t.Fatal(c)
	}
	c, v := call(s, "POST", "auth/otp/verify", "", `{"phone":"+919876543210","otp":"1234"}`)
	if c != 200 {
		t.Fatal(c, v)
	}
	token := v["token"].(string)
	if c, _ := call(s, "POST", "purchase", "", `{"request_id":"payment-test-1"}`); c != 401 {
		t.Fatal(c)
	}
	for i := 0; i < 2; i++ {
		c, v = call(s, "POST", "purchase", token, `{"request_id":"payment-test-1"}`)
		if c != 200 || v["unlocked"] != true {
			t.Fatal(c, v)
		}
	}
	if len(v["history"].([]any)) != 2 {
		t.Fatal("duplicate payment", v)
	}
	call(s, "POST", "activity/puja", token, steps)
	restored, _ := New(path)
	_, v = call(restored, "GET", "me", token, "")
	if v["streak"] != float64(1) || len(v["history"].([]any)) != 3 || v["phone"] != "9876543210" {
		t.Fatal(v)
	}
	restored.MockMode = true
	_, login := call(restored, "POST", "auth/otp/verify", "", `{"phone":"9876543210","otp":"1234"}`)
	if login["profile"].(map[string]any)["id"] != v["id"] {
		t.Fatal("new account on repeat login")
	}
	if c, _ := call(restored, "DELETE", "me", token, ""); c != 200 {
		t.Fatal(c)
	}
	if c, _ := call(restored, "GET", "me", token, ""); c != 401 {
		t.Fatal(c)
	}
	if len(restored.db.Accounts) != 0 {
		t.Fatal("phone account not deleted")
	}
}
