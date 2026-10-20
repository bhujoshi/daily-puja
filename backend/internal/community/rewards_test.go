package community

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/worship/nityamandir/backend/internal/googleauth"
)

func googleLogin(t *testing.T, s *Server, subject, device string) string {
	t.Helper()
	g := &googleStub{identity: googleauth.Identity{Subject: subject, Email: subject + "@gmail.com"}}
	s.Google = g
	_, challenge := call(s, "POST", "auth/google/challenge", "", `{}`)
	g.identity.Nonce = challenge["nonce"].(string)
	b, _ := json.Marshal(map[string]string{"id_token": "test", "nonce": g.identity.Nonce, "install_id": device})
	code, response := call(s, "POST", "auth/google", "", string(b))
	if code != 200 {
		t.Fatal(code, response)
	}
	return response["token"].(string)
}

func shareLink(t *testing.T, s *Server, token, request string) string {
	t.Helper()
	code, response := call(s, "POST", "shares", token, `{"request_id":"`+request+`"}`)
	if code != 200 {
		t.Fatal(code, response)
	}
	id := response["share_id"].(string)
	if !strings.Contains(response["url"].(string), "referrer=share_id%3D"+id) {
		t.Fatal(response)
	}
	return id
}

func TestCoinWalletAndIndividualUnlocks(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	now := time.Date(2026, 10, 11, 18, 29, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	_, auth := call(s, "POST", "register", "", `{"email":"coins@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	for i := 0; i < 9; i++ {
		call(s, "POST", "activity/puja", token, steps)
		_, profile := call(s, "POST", "activity/puja", token, steps)
		if profile["coins"] != float64(i+1) {
			t.Fatal("duplicate daily coin", profile)
		}
		// Include missed days: coins must accumulate independently of streaks.
		now = now.Add(48 * time.Hour)
	}
	for _, item := range []string{"ivory", "ivory"} {
		code, profile := call(s, "POST", "coins/unlock", token, `{"category":"shrine","item":"`+item+`"}`)
		if code != 200 || profile["coins"] != float64(2) {
			t.Fatal("double debit", code, profile)
		}
	}
	if code, _ := call(s, "POST", "coins/unlock", token, `{"category":"shrine","item":"carved"}`); code != 409 {
		t.Fatal("overspending", code)
	}
	code, profile := call(s, "POST", "coins/unlock", token, `{"category":"flowers","item":"rose"}`)
	if code != 200 || profile["coins"] != float64(0) || profile["unlocked"] != false {
		t.Fatal(code, profile)
	}
	selection := `{"selections":{"shrine":"ivory","idols":"shiva","flowers":"original,rose","lamp":"brass","shankh":"ivory","aarti":"traditional","prasad":"halwa"}}`
	if code, _ := call(s, "PUT", "shrine", token, selection); code != 200 {
		t.Fatal(code)
	}
	if code, _ := call(s, "PUT", "shrine", token, strings.Replace(selection, `"ivory"`, `"carved"`, 1)); code != 403 {
		t.Fatal("unowned shrine accepted", code)
	}
	if code, _ := call(s, "PUT", "shrine", token, strings.Replace(selection, `original,rose`, `rose,orchid`, 1)); code != 403 {
		t.Fatal("unowned flower accepted", code)
	}
	if code, _ := call(s, "POST", "coins/unlock", token, `{"category":"unknown","item":"original"}`); code != 400 {
		t.Fatal(code)
	}
	restored, err := New(s.path)
	if err != nil {
		t.Fatal(err)
	}
	_, profile = call(restored, "GET", "me", token, "")
	if profile["coins"] != float64(0) || profile["owned"].(map[string]any)["shrine:ivory"] != true || profile["selections"].(map[string]any)["flowers"] != "original,rose" {
		t.Fatal(profile)
	}
}

func TestFreeRitualVariantsWithoutPackage(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"free@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	selection := `{"selections":{"shrine":"original","idols":"durga","flowers":"original","lamp":"brass","shankh":"ivory","aarti":"traditional","prasad":"laddu"}}`
	code, profile := call(s, "PUT", "shrine", token, selection)
	if code != 200 || profile["coins"] != float64(0) || profile["unlocked"] != false {
		t.Fatal(code, profile)
	}
}

func TestDefaultMarbleShrineNeedsNoCoinPurchase(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"marble@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	selection := `{"selections":{"shrine":"marble","idols":"durga","flowers":"original","lamp":"brass","shankh":"ivory","aarti":"traditional","prasad":"laddu"}}`
	code, profile := call(s, "PUT", "shrine", token, selection)
	if code != 200 || profile["coins"] != float64(0) || profile["unlocked"] != false {
		t.Fatal("default shrine blocked a free selection", code, profile)
	}
	// Even a legacy client sending an explicit unlock must not debit the wallet.
	s.db.Accounts["marble@example.com"].Coins = 7
	code, profile = call(s, "POST", "coins/unlock", token, `{"category":"shrine","item":"marble"}`)
	if code != 200 || profile["coins"] != float64(7) || profile["owned"].(map[string]any)["shrine:marble"] == true {
		t.Fatal("free default consumed coins", code, profile)
	}
}

func TestPaidFlowerCanBeAppliedWithDefaultMarbleShrine(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"flower@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	s.db.Accounts["flower@example.com"].Coins = 2
	selection := `{"selections":{"shrine":"marble","idols":"original","flowers":"original,rose","lamp":"original","shankh":"original","aarti":"original","prasad":"original"}}`
	if code, _ := call(s, "PUT", "shrine", token, selection); code != 403 {
		t.Fatal("unowned flower accepted", code)
	}
	code, profile := call(s, "POST", "coins/unlock", token, `{"category":"flowers","item":"rose"}`)
	if code != 200 || profile["coins"] != float64(0) || profile["owned"].(map[string]any)["flowers:rose"] != true {
		t.Fatal("flower purchase failed", code, profile)
	}
	code, profile = call(s, "PUT", "shrine", token, selection)
	if code != 200 || profile["selections"].(map[string]any)["flowers"] != "original,rose" {
		t.Fatal("purchased flower could not be applied", code, profile)
	}
}

func TestGarlandUnlockForFourCoins(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"garland@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	s.db.Accounts["garland@example.com"].Coins = 4
	selection := `{"selections":{"shrine":"marble","idols":"original","garland":"lotus","flowers":"original","lamp":"original","shankh":"original","aarti":"original","prasad":"original"}}`
	if code, _ := call(s, "PUT", "shrine", token, selection); code != 403 {
		t.Fatal("unowned garland accepted", code)
	}
	code, profile := call(s, "POST", "coins/unlock", token, `{"category":"garland","item":"lotus"}`)
	if code != 200 || profile["coins"] != float64(0) || profile["owned"].(map[string]any)["garland:lotus"] != true {
		t.Fatal("garland purchase failed", code, profile)
	}
	code, profile = call(s, "PUT", "shrine", token, selection)
	if code != 200 || profile["selections"].(map[string]any)["garland"] != "lotus" {
		t.Fatal("purchased garland could not be applied", code, profile)
	}
}

func TestIncenseAndRitualUpgradeUnlocks(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"incense@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	s.db.Accounts["incense@example.com"].Coins = 6
	selection := `{"selections":{"shrine":"marble","idols":"original","garland":"original","flowers":"original","incense":"kamal_dhoop","lamp":"peacock","shankh":"original","aarti":"original","prasad":"original"}}`
	if code, _ := call(s, "PUT", "shrine", token, selection); code != 403 {
		t.Fatal("unowned incense or lamp accepted", code)
	}
	code, profile := call(s, "POST", "coins/unlock", token, `{"category":"incense","item":"kamal_dhoop"}`)
	if code != 200 || profile["coins"] != float64(3) || profile["owned"].(map[string]any)["incense:kamal_dhoop"] != true {
		t.Fatal("incense purchase failed", code, profile)
	}
	code, profile = call(s, "POST", "coins/unlock", token, `{"category":"lamp","item":"peacock"}`)
	if code != 200 || profile["coins"] != float64(0) || profile["owned"].(map[string]any)["lamp:peacock"] != true {
		t.Fatal("peacock lamp purchase failed", code, profile)
	}
	code, profile = call(s, "PUT", "shrine", token, selection)
	if code != 200 || profile["selections"].(map[string]any)["incense"] != "kamal_dhoop" || profile["selections"].(map[string]any)["lamp"] != "peacock" {
		t.Fatal("purchased incense and lamp could not be applied", code, profile)
	}
}


func TestGoogleSharingAndInstallRewards(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	now := time.Date(2026, 10, 11, 12, 0, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	_, auth := call(s, "POST", "register", "", `{"email":"legacy@example.com","password":"long-password"}`)
	if code, _ := call(s, "POST", "shares", auth["token"].(string), `{"request_id":"share-legacy"}`); code != 403 {
		t.Fatal(code)
	}
	owner := googleLogin(t, s, "owner", strings.Repeat("a", 64))
	id := shareLink(t, s, owner, "share-request-1")
	if again := shareLink(t, s, owner, "share-request-1"); again != id {
		t.Fatal("non-idempotent share")
	}
	for i := 0; i < 2; i++ {
		call(s, "POST", "shares/opened", owner, `{"share_id":"`+id+`"}`)
	}
	_, profile := call(s, "GET", "me", owner, "")
	if profile["coins"] != float64(0) || profile["share_count"] != float64(1) {
		t.Fatal("handoff earned coins or counted twice", profile)
	}
	now = now.Add(time.Minute)
	friend := googleLogin(t, s, "friend", strings.Repeat("b", 64))
	claim, _ := json.Marshal(map[string]any{"share_id": id, "install_id": strings.Repeat("b", 64), "installed_at": now.Unix()})
	for i := 0; i < 3; i++ {
		code, response := call(s, "POST", "referrals/install", friend, string(claim))
		if code != 200 || response["credited"] != (i == 0) {
			t.Fatal(code, response)
		}
	}
	call(s, "POST", "activity/puja", friend, steps)
	_, profile = call(s, "GET", "me", owner, "")
	if profile["coins"] != float64(1) || profile["referrals"] != float64(1) || profile["unlocked"] != false {
		t.Fatal(profile)
	}
	// Each distinct new user earns one more coin, even using the same link.
	second := googleLogin(t, s, "second", strings.Repeat("c", 64))
	claim2 := strings.Replace(string(claim), strings.Repeat("b", 64), strings.Repeat("c", 64), 1)
	if code, response := call(s, "POST", "referrals/install", second, claim2); code != 200 || response["credited"] != true {
		t.Fatal(code, response)
	}
	call(s, "DELETE", "me", friend, "")
	friend = googleLogin(t, s, "friend", strings.Repeat("d", 64))
	claim3 := strings.Replace(string(claim), strings.Repeat("b", 64), strings.Repeat("d", 64), 1)
	if code, response := call(s, "POST", "referrals/install", friend, claim3); code != 200 || response["credited"] != false {
		t.Fatal("account recreation rewarded", code, response)
	}
	restored, _ := New(s.path)
	_, profile = call(restored, "GET", "me", owner, "")
	if profile["coins"] != float64(2) || profile["referrals"] != float64(2) {
		t.Fatal(profile)
	}
}

func TestReferralRejectsExistingSelfAndSameDevice(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	now := time.Date(2026, 10, 11, 12, 0, 0, 0, time.UTC)
	s.now = func() time.Time { return now }
	ownerDevice := strings.Repeat("a", 64)
	owner := googleLogin(t, s, "owner", ownerDevice)
	existingDevice := strings.Repeat("b", 64)
	existing := googleLogin(t, s, "existing", existingDevice)
	now = now.Add(time.Minute)
	id := shareLink(t, s, owner, "share-test-1")
	same := googleLogin(t, s, "same-device", ownerDevice)
	for _, test := range []struct{ token, device string }{{owner, ownerDevice}, {existing, existingDevice}, {same, ownerDevice}} {
		b, _ := json.Marshal(map[string]any{"share_id": id, "install_id": test.device, "installed_at": now.Unix()})
		code, response := call(s, "POST", "referrals/install", test.token, string(b))
		if code != 200 || response["credited"] != false {
			t.Fatal("ineligible installation rewarded", code, response)
		}
	}
	newDevice := strings.Repeat("c", 64)
	newUser := googleLogin(t, s, "new", newDevice)
	for _, installed := range []int64{now.Add(-time.Hour).Unix(), now.Add(time.Hour).Unix()} {
		b, _ := json.Marshal(map[string]any{"share_id": id, "install_id": newDevice, "installed_at": installed})
		code, response := call(s, "POST", "referrals/install", newUser, string(b))
		if code != 200 || response["credited"] != false {
			t.Fatal("invalid install date rewarded", code, response)
		}
	}
}

func TestCoinMutationRollbackAndConcurrentUnlock(t *testing.T) {
	s, _ := New(filepath.Join(t.TempDir(), "accounts.json"))
	_, auth := call(s, "POST", "register", "", `{"email":"race@example.com","password":"long-password"}`)
	token := auth["token"].(string)
	s.db.Accounts["race@example.com"].Coins = 7
	path := s.path
	s.path = t.TempDir()
	if code, _ := call(s, "POST", "coins/unlock", token, `{"category":"shrine","item":"carved"}`); code != 503 {
		t.Fatal(code)
	}
	if a := s.db.Accounts["race@example.com"]; a.Coins != 7 || a.Owned["shrine:carved"] {
		t.Fatal("failed persistence debited wallet", a)
	}
	s.path = path
	var wg sync.WaitGroup
	codes := make(chan int, 2)
	for _, item := range []string{"carved", "ivory"} {
		wg.Add(1)
		go func(item string) {
			defer wg.Done()
			code, _ := call(s, "POST", "coins/unlock", token, `{"category":"shrine","item":"`+item+`"}`)
			codes <- code
		}(item)
	}
	wg.Wait()
	close(codes)
	counts := map[int]int{}
	for code := range codes {
		counts[code]++
	}
	if counts[200] != 1 || counts[409] != 1 || s.db.Accounts["race@example.com"].Coins != 0 {
		t.Fatal(counts)
	}
}

func TestLegacyWalletMigration(t *testing.T) {
	path := filepath.Join(t.TempDir(), "accounts.json")
	legacy := `{"Accounts":{"legacy":{"id":"legacy","days":["2026-10-09","2026-10-09","2026-10-10"],"referrals":1,"unlocked":true}},"Sessions":{}}`
	if err := os.WriteFile(path, []byte(legacy), 0600); err != nil {
		t.Fatal(err)
	}
	s, err := New(path)
	if err != nil {
		t.Fatal(err)
	}
	a := s.db.Accounts["legacy"]
	if a.Coins != 3 || !owns(a, "shrine", "carved") || a.WalletVersion != 1 {
		t.Fatal(a)
	}
	if err := s.save(); err != nil {
		t.Fatal(err)
	}
	s, err = New(path)
	if err != nil || s.db.Accounts["legacy"].Coins != 3 {
		t.Fatal("migration credited twice", err)
	}
}
