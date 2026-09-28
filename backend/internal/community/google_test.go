package community

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/worship/nityamandir/backend/internal/googleauth"
	"path/filepath"
	"testing"
	"time"
)

type googleStub struct {
	identity googleauth.Identity
	err      error
}

func (g *googleStub) Verify(context.Context, string) (googleauth.Identity, error) {
	return g.identity, g.err
}
func TestGoogleAccountAndChallenges(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	if c, _ := call(s, "POST", "auth/google/challenge", "", `{}`); c != 503 {
		t.Fatal(c)
	}
	g := &googleStub{identity: googleauth.Identity{Subject: "google-subject", Email: "bhakt@example.com"}}
	s.Google = g
	challenge := func() string {
		c, v := call(s, "POST", "auth/google/challenge", "", `{}`)
		if c != 200 {
			t.Fatal(c, v)
		}
		return v["nonce"].(string)
	}
	verify := func(nonce string) (int, map[string]any) {
		b, _ := json.Marshal(map[string]string{"id_token": "stub-token", "nonce": nonce})
		return call(s, "POST", "auth/google", "", string(b))
	}
	nonce := challenge()
	g.identity.Nonce = "wrong-nonce"
	if c, _ := verify(nonce); c != 401 {
		t.Fatal("nonce mismatch accepted", c)
	}
	g.identity.Nonce = nonce
	g.err = errors.New("invalid signature")
	if c, _ := verify(nonce); c != 401 || len(s.db.Accounts) != 0 {
		t.Fatal("unverified account created", c)
	}
	g.err = nil
	c, v := verify(nonce)
	if c != 200 {
		t.Fatal(c, v)
	}
	token := v["token"].(string)
	accountID := v["profile"].(map[string]any)["id"]
	if c, _ := verify(nonce); c != 401 {
		t.Fatal("replayed challenge accepted", c)
	}
	restored, _ := New(s.path)
	if c, _ := call(restored, "GET", "me", token, ""); c != 200 {
		t.Fatal("session not persisted", c)
	}
	nonce = challenge()
	g.identity.Nonce = nonce
	g.identity.Email = "changed@example.com"
	c, v = verify(nonce)
	if c != 200 || v["profile"].(map[string]any)["id"] != accountID {
		t.Fatal("email change created new account", c, v)
	}
	nonce = challenge()
	g.identity.Nonce = nonce
	s.googleNonces[nonce] = s.now().Add(-time.Second)
	if c, _ := verify(nonce); c != 401 {
		t.Fatal("expired challenge accepted", c)
	}
	if c, _ := call(s, "DELETE", "me", token, ""); c != 200 {
		t.Fatal(c)
	}
	if c, _ := call(s, "GET", "me", token, ""); c != 401 {
		t.Fatal("deleted account still signed in", c)
	}
}

func TestOwnerPackageGrant(t *testing.T) {
	for _, email := range []string{"bhuwanchandra.it@gmail.com", "BHUWANCHANDRA.IT@gmail.com", "other@gmail.com"} {
		t.Run(email, func(t *testing.T) {
			s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
			g := &googleStub{identity: googleauth.Identity{Subject: "owner-subject", Email: email}}
			s.Google = g
			var token string
			for i := 0; i < 2; i++ {
				_, challenge := call(s, "POST", "auth/google/challenge", "", `{}`)
				g.identity.Nonce = challenge["nonce"].(string)
				body, _ := json.Marshal(map[string]string{"id_token": "stub-token", "nonce": g.identity.Nonce})
				code, response := call(s, "POST", "auth/google", "", string(body))
				want := email != "other@gmail.com"
				if code != 200 || response["profile"].(map[string]any)["unlocked"] != want {
					t.Fatal(code, response)
				}
				token = response["token"].(string)
			}
			grants := 0
			for _, event := range s.db.Accounts["google:owner-subject"].History {
				if event.Kind == "package_granted" {
					grants++
				}
			}
			wantGrants := 1
			if email == "other@gmail.com" {
				wantGrants = 0
			}
			if grants != wantGrants {
				t.Fatal("duplicate or unexpected grant", grants)
			}
			restored, err := New(s.path)
			if err != nil {
				t.Fatal(err)
			}
			code, response := call(restored, "GET", "me", token, "")
			if code != 200 || response["unlocked"] != (wantGrants == 1) {
				t.Fatal("grant not persisted", code, response)
			}
		})
	}
}
