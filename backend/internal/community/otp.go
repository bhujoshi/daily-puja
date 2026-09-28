package community

import (
	"errors"
	"net/http"
	"strings"

	"github.com/worship/nityamandir/backend/internal/otp"
)

func (s *Server) authorizeOTP(w http.ResponseWriter, r *http.Request, raw, code string, sending bool) bool {
	if s.OTP == nil && !s.MockMode {
		fail(w, 503, "Mobile login is not configured.")
		return false
	}
	phone := strings.TrimPrefix(strings.TrimSpace(raw), "+91")
	if len(phone) != 10 || phone[0] < '6' || phone[0] > '9' || strings.Trim(phone, "0123456789") != "" {
		fail(w, 400, "Enter a valid 10-digit Indian mobile number")
		return false
	}
	if s.OTP == nil {
		if sending {
			reply(w, 200, map[string]any{"mock": true, "message": "Demo only: use OTP 1234. No SMS sent."})
			return true
		}
		if code != "1234" {
			fail(w, 401, "Incorrect demo OTP. Use 1234.")
			return false
		}
		return true
	}
	if !sending && (len(code) < 4 || len(code) > 10 || strings.Trim(code, "0123456789") != "") {
		fail(w, 400, "Enter the numeric OTP received on your phone")
		return false
	}
	err := s.OTP.Allow(phone, r.RemoteAddr, sending)
	if err == nil {
		if sending {
			err = s.OTP.Request(r.Context(), "+91"+phone)
		} else {
			err = s.OTP.Verify(r.Context(), "+91"+phone, code)
		}
	}
	if err != nil {
		status := 503
		if errors.Is(err, otp.ErrInvalid) {
			status = 401
		}
		if errors.Is(err, otp.ErrLimited) {
			status = 429
			w.Header().Set("Retry-After", "60")
		}
		// Provider errors are deliberately not exposed to clients or logs.
		message := otp.ErrUnavailable.Error()
		if status == 401 {
			message = otp.ErrInvalid.Error()
		}
		if status == 429 {
			message = otp.ErrLimited.Error()
		}
		fail(w, status, message)
		return false
	}
	if sending {
		reply(w, 200, map[string]any{"mock": false, "message": "OTP sent to your phone.", "resend_after_seconds": 60})
	}
	return true
}
