package community

import (
	"net/http"
	"net/url"
	"regexp"
	"time"
)

type rewardRequest struct {
	IDToken     string            `json:"id_token"`
	Nonce       string            `json:"nonce"`
	Phone       string            `json:"phone"`
	OTP         string            `json:"otp"`
	RequestID   string            `json:"request_id"`
	Email       string            `json:"email"`
	Password    string            `json:"password"`
	Invite      string            `json:"invite_code"`
	Steps       []string          `json:"steps"`
	Selections  map[string]string `json:"selections"`
	Category    string            `json:"category"`
	Item        string            `json:"item"`
	ShareID     string            `json:"share_id"`
	InstallID   string            `json:"install_id"`
	InstalledAt int64             `json:"installed_at"`
}

type Share struct {
	Owner     string    `json:"owner"`
	RequestID string    `json:"request_id"`
	CreatedAt time.Time `json:"created_at"`
	Opened    bool      `json:"opened"`
}

func (s *Server) initRewards() {
	if s.db.Accounts == nil {
		s.db.Accounts = map[string]*Account{}
	}
	if s.db.Sessions == nil {
		s.db.Sessions = map[string]Session{}
	}
	if s.db.Shares == nil {
		s.db.Shares = map[string]*Share{}
	}
	if s.db.ReferralUsers == nil {
		s.db.ReferralUsers = map[string]bool{}
	}
	if s.db.ReferralInstalls == nil {
		s.db.ReferralInstalls = map[string]bool{}
	}
	if s.db.InstallUsers == nil {
		s.db.InstallUsers = map[string]string{}
	}
	for _, a := range s.db.Accounts {
		if a.Owned == nil {
			a.Owned = map[string]bool{}
		}
		if a.WalletVersion == 0 {
			seen := map[string]bool{}
			for _, day := range a.Days {
				seen[day] = true
			}
			a.Coins = len(seen) + a.Referrals
			a.WalletVersion = 1
		}
	}
}

func coinCost(category, item string) int {
	if item == "original" || (category == "shrine" && item == "marble") {
		return 0
	}
	switch category {
	case "shrine":
		return 7
	case "garland":
		return 4
	case "flowers":
		return 2
	default:
		return 0
	}
}

func owns(a *Account, category, item string) bool {
	return a.Unlocked || coinCost(category, item) == 0 || a.Owned[category+":"+item]
}

var installPattern = regexp.MustCompile(`^[a-f0-9]{64}$`)

// Called under the same lock and atomic commit as account operations. A share
// handoff is observable, but WhatsApp does not report delivery or message sends.
func (s *Server) rewards(w http.ResponseWriter, r *http.Request, path string, a *Account, body rewardRequest, commit func() bool) bool {
	if r.Method != "POST" {
		return false
	}
	switch path {
	case "coins/unlock":
		if !catalogItem(body.Category, body.Item) {
			fail(w, 400, "Choose an available catalog item")
			return true
		}
		if !owns(a, body.Category, body.Item) {
			cost := coinCost(body.Category, body.Item)
			if a.Coins < cost {
				fail(w, 409, "Not enough coins. Complete daily puja or invite friends.")
				return true
			}
			a.Coins -= cost
			if a.Owned == nil {
				a.Owned = map[string]bool{}
			}
			a.Owned[body.Category+":"+body.Item] = true
			s.event(a, "coin_unlock", body.Category+":"+body.Item)
		}
		if commit() {
			reply(w, 200, s.profile(a))
		}
	case "shares":
		if a.GoogleSubject == "" {
			fail(w, 403, "Sign in with Google before sharing")
			return true
		}
		if len(body.RequestID) < 8 || len(body.RequestID) > 128 {
			fail(w, 400, "A share request ID is required")
			return true
		}
		id := ""
		for key, share := range s.db.Shares {
			if share.Owner == a.ID && share.RequestID == body.RequestID {
				id = key
				break
			}
		}
		if id == "" {
			id = random()
			s.db.Shares[id] = &Share{Owner: a.ID, RequestID: body.RequestID, CreatedAt: s.now()}
		}
		link := "https://play.google.com/store/apps/details?id=com.pavitramandir.app&referrer=" + url.QueryEscape("share_id="+id)
		if commit() {
			reply(w, 200, map[string]any{"share_id": id, "url": link})
		}
	case "shares/opened":
		share := s.db.Shares[body.ShareID]
		if share == nil || share.Owner != a.ID {
			fail(w, 404, "Invitation not found")
			return true
		}
		if !share.Opened {
			share.Opened = true
			a.ShareCount++
			s.event(a, "whatsapp_opened", "WhatsApp invitation opened")
		}
		if commit() {
			reply(w, 200, s.profile(a))
		}
	case "referrals/install":
		if a.GoogleSubject == "" {
			fail(w, 403, "Sign in with Google to register an installation")
			return true
		}
		share := s.db.Shares[body.ShareID]
		if !installPattern.MatchString(body.InstallID) || body.InstalledAt <= 0 {
			fail(w, 400, "Invalid installation referral")
			return true
		}
		if share == nil {
			reply(w, 200, map[string]bool{"processed": true, "credited": false})
			return true
		}
		userKey := digest("referral-google:" + a.GoogleSubject)
		installKey := digest("referral-install:" + body.InstallID)
		// Stable Google subject and installation identity survive account deletion
		// as digests, so deleting an account cannot claim the reward again.
		if s.db.ReferralUsers[userKey] || s.db.ReferralInstalls[installKey] {
			reply(w, 200, map[string]bool{"processed": true, "credited": false})
			return true
		}
		installed := time.Unix(body.InstalledAt, 0)
		if !a.ReferralEligible || a.InstallID != installKey || share.Owner == a.ID || a.CreatedAt.IsZero() || a.CreatedAt.Before(share.CreatedAt) ||
			installed.Unix() < share.CreatedAt.Unix() || installed.After(s.now().Add(time.Minute)) || installed.After(a.CreatedAt.Add(time.Minute)) {
			reply(w, 200, map[string]bool{"processed": true, "credited": false})
			return true
		}
		var owner *Account
		for _, other := range s.db.Accounts {
			if other.ID == share.Owner {
				owner = other
				break
			}
		}
		if owner == nil {
			reply(w, 200, map[string]bool{"processed": true, "credited": false})
			return true
		}
		s.db.ReferralUsers[userKey] = true
		s.db.ReferralInstalls[installKey] = true
		a.Referrer = owner.ID
		owner.Coins++
		owner.Referrals++
		s.event(owner, "referral_coin", "New referred installation · +1 coin")
		if commit() {
			reply(w, 200, map[string]bool{"processed": true, "credited": true})
		}
	default:
		return false
	}
	return true
}
