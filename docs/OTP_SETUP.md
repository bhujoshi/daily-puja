# WhatsApp OTP setup

The existing `POST /api/v2/auth/otp/request` and `POST /api/v2/auth/otp/verify`
support Fast2SMS Smart OTP and Twilio Verify. Provider calls happen outside the account database lock.

## Fast2SMS WhatsApp (recommended for this project)

1. Sign up at [Fast2SMS](https://www.fast2sms.com/) and verify your account.
2. Open **WhatsApp Business / WhatsApp Manager** and connect a business number
   through the guided Meta/Facebook signup. Select or create your Meta business
   portfolio and WhatsApp Business Account; complete the number verification.
   Use a number you control and follow the dashboard's eligibility instructions.
   [Connection guide](https://www.fast2sms.com/help/fast2sms-whatsapp-business-api-features-pricing-quick-integration/).
3. Create a WhatsApp **Authentication** template for login codes (for example,
   `pavitra_mandir_login`, English, Copy Code button). Wait for Meta approval.
   Set any displayed expiry text to 5 minutes to match this backend.
4. Open **Smart OTP → Add OTP Template**. Choose WhatsApp as the primary channel,
   select your connected number and approved authentication template, and select
   the variable that carries the OTP. Assign the template and copy its **OTP ID**.
   Leave SMS fallback disabled for WhatsApp-only delivery. SMS fallback needs DLT
   registration and has its own charges.
   [Smart OTP setup](https://www.fast2sms.com/help/whatsapp-otp-verification-api/).
5. Copy your API key from **Dev API**. Put both values in the backend's private
   deployment `.env` (not in Android or a chat message):

   ```dotenv
   MOCK_MODE=false
   OTP_PROVIDER=fast2sms
   FAST2SMS_API_KEY=your_private_api_key
   FAST2SMS_OTP_ID=your_assigned_otp_id
   ```

   `OTP_CHANNEL` is only used by Twilio. Fast2SMS uses the channel and fallback
   configured on your OTP ID in its dashboard.
6. Confirm available wallet/test balance and send a test from the Smart OTP panel
   to your own WhatsApp number. After it works, from `backend/deploy` run:

   ```sh
   docker compose up -d --build api
   ```

   For local Go development, export the variables before `go run ./cmd/server`;
   the Go server does not automatically read `.env` files.
7. Test from Android, or request and verify against the running backend:

   ```sh
   curl -sS http://127.0.0.1:8080/api/v2/auth/otp/request \
     -H 'Content-Type: application/json' \
     -d '{"phone":"YOUR_10_DIGIT_NUMBER"}'
   curl -sS http://127.0.0.1:8080/api/v2/auth/otp/verify \
     -H 'Content-Type: application/json' \
     -d '{"phone":"YOUR_10_DIGIT_NUMBER","otp":"RECEIVED_CODE"}'
   ```

   Verification returns the existing account token and profile. Requests support
   a `+91` prefix; the adapter sends Fast2SMS the required 10-digit number.
   Fast2SMS generates six-digit codes with a five-minute expiry and verifies them
   server-side. The backend never stores the codes. Request acceptance does not
   guarantee message delivery; check Fast2SMS delivery logs if a code does not arrive.

Troubleshooting: missing/invalid key, OTP ID, wallet balance or approved sender
can cause 503 responses. Provider error details are deliberately hidden from app
users; check the Fast2SMS panel. Invalid/expired/already-used codes return 401;
rate limits return 429. Live OTP cannot run alongside `MOCK_MODE=true`.

## Alternative: Twilio Verify

1. Create a Twilio Verify Service and configure your WhatsApp sender using
   [Twilio's WhatsApp setup](https://www.twilio.com/docs/verify/whatsapp).
   WhatsApp requires your own sender associated with a WhatsApp Business Account.
2. Put these values in `backend/deploy/.env` (never commit credentials):

   ```dotenv
   MOCK_MODE=false
   OTP_PROVIDER=twilio
   OTP_CHANNEL=whatsapp
   TWILIO_ACCOUNT_SID=AC...
   TWILIO_AUTH_TOKEN=...
   TWILIO_VERIFY_SERVICE_SID=VA...
   ```

3. From `backend/deploy`, run `docker compose up -d --build api`.
   For local `go run ./cmd/server`, export the same variables first; Go does not
   automatically load `.env` files.
4. Request a code with `{"phone":"9876543210"}`, then verify with
   `{"phone":"9876543210","otp":"123456"}`. A `+91` prefix is also accepted.
   Verification returns the existing `token` and `profile` response and supports
   an optional `invite_code`. Android accepts codes of 4–10 digits.

Twilio generates and expires codes and approves successful verification. Codes
and provider credentials are never returned by the backend. Only `approved`
verification creates a session; invalid/expired codes return 401, throttling 429,
and provider failures 503. Requests return a 60-second resend interval.

Local limits: 5 sends per phone/hour, 30 sends per connecting IP/hour, 20 checks
per phone/hour, and 100 checks per connecting IP/hour. Limits are in memory and
reset on restart. Behind a reverse proxy, the connecting IP is the proxy, so IP
limits are shared; add client-aware rate limiting at the trusted proxy and
provider abuse controls before public use. Account storage still supports one
server process. Fast2SMS fallback is controlled by its dashboard; Twilio fallback is not implemented.

With no `OTP_PROVIDER`, live verification stays disabled. `MOCK_MODE=true` retains
the development-only code `1234`; startup rejects live OTP combined with mock mode.

Run `cd backend && go test ./...`. Tests use an HTTP stub; actual WhatsApp delivery
must be checked with your configured provider account and a consenting test number.
