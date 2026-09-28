package googleauth

import (
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"google.golang.org/api/idtoken"
	"math/big"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

type transportFunc func(*http.Request) (*http.Response, error)

func (f transportFunc) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }
func TestSignedGoogleIdentity(t *testing.T) {
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatal(err)
	}
	enc := base64.RawURLEncoding.EncodeToString
	client := &http.Client{Transport: transportFunc(func(r *http.Request) (*http.Response, error) {
		if r.URL.String() != "https://www.googleapis.com/oauth2/v3/certs" {
			t.Fatal(r.URL)
		}
		w := httptest.NewRecorder()
		w.Header().Set("Cache-Control", "max-age=3600")
		_ = json.NewEncoder(w).Encode(map[string]any{"keys": []any{map[string]string{"kid": "key", "kty": "RSA", "alg": "RS256", "n": enc(key.N.Bytes()), "e": enc(big.NewInt(int64(key.E)).Bytes())}}})
		return w.Result(), nil
	})}
	validator, err := idtoken.NewValidator(context.Background(), idtoken.WithHTTPClient(client))
	if err != nil {
		t.Fatal(err)
	}
	service := &Service{audience: "web.apps.googleusercontent.com", validate: validator.Validate}
	sign := func(c map[string]any) string {
		header, _ := json.Marshal(map[string]string{"alg": "RS256", "kid": "key"})
		payload, _ := json.Marshal(c)
		content := enc(header) + "." + enc(payload)
		sum := sha256.Sum256([]byte(content))
		sig, e := rsa.SignPKCS1v15(rand.Reader, key, crypto.SHA256, sum[:])
		if e != nil {
			t.Fatal(e)
		}
		return content + "." + enc(sig)
	}
	claims := map[string]any{"iss": "https://accounts.google.com", "aud": service.audience, "exp": time.Now().Add(time.Hour).Unix(), "sub": "subject-1", "email": "bhakt@example.com", "email_verified": true, "nonce": "challenge-1"}
	identity, err := service.Verify(context.Background(), sign(claims))
	if err != nil || identity.Subject != "subject-1" || identity.Nonce != "challenge-1" {
		t.Fatal(identity, err)
	}
	for k, v := range map[string]any{"aud": "other-client", "iss": "https://attacker.example", "exp": time.Now().Add(-time.Minute).Unix(), "email_verified": false, "sub": "", "nonce": ""} {
		old := claims[k]
		claims[k] = v
		if _, err := service.Verify(context.Background(), sign(claims)); err == nil {
			t.Fatal("invalid claim accepted", k)
		}
		claims[k] = old
	}
	token := sign(claims)
	token = token[:len(token)-10] + "AAAAAAAAAA"
	if _, err := service.Verify(context.Background(), token); err == nil {
		t.Fatal("invalid signature accepted")
	}
	if _, err := service.Verify(context.Background(), "not-a-jwt"); err == nil {
		t.Fatal("invalid token accepted")
	}
}
