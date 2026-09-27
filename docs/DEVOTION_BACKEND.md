# Devotion accounts pilot

Implemented for Android and the Go backend; iOS and web remain unchanged. This is a local backend foundation, not a deployed payment or asset store.

## Run and connect

From `backend`, run `MOCK_MODE=true go run ./cmd/server` for the local demo. Accounts persist at `data/accounts.json` (override with `ACCOUNT_DATA_PATH`). The server binds loopback on port 8080; use a TLS reverse proxy for a device connection. Build Android with `./gradlew assembleDebug -PaccountApiUrl=https://YOUR_API_HOST`. Without a URL the app explicitly says accounts are not connected, while puja continues offline.

Storage is an atomic, owner-only JSON file for **one server process**. Back up securely. Migrate to transactional SQL, add monitoring, request throttling at the proxy, verified identity, account recovery and backup deletion policy before public deployment. Mobile login accepts a 10-digit Indian number (or +91 prefix) and fixed OTP **1234** only when `MOCK_MODE=true`. No SMS is sent. This is unverified demo identity, not production authentication. Sessions expire after 30 days, are hashed on the server and encrypted with Android Keystore on the device. Reopening restores the session and cached profile, including shrine selections, credited days, streak and activity/payment history. Android backup is disabled for this account cache. Refresh fetches server state; a rejected/expired session clears the local login. Do not enable cleartext HTTP for release. Debug builds can connect to `http://10.0.2.2:8080` using the debug-only local network configuration.

Legacy `/api/v1` prototype endpoints trust supplied user IDs and dates. They are disabled by default; `ENABLE_LEGACY_DEMO=true` explicitly enables them for development only. Their old streak state is separate from the account service.

## API

All paths below start `/api/v2`. JSON errors use `error`. Authenticated requests require `Authorization: Bearer TOKEN`.

| Method/path | Body / behavior |
| --- | --- |
| POST /auth/otp/request | `phone`; validates mobile number, demo notice, sends no SMS |
| POST /auth/otp/verify | `phone`, `otp: "1234"`, optional `invite_code`; creates or signs into the same mobile account |
| POST /register | `email`, `password` (10–128 chars), optional `invite_code`; returns token/profile |
| POST /login | email/password; returns token/profile |
| GET /catalog | Seven categories, reward thresholds and availability flags |
| GET /me | Mobile, account ID, streak, credited days, invite code, qualified referrals, selections, entitlement, activity/payment history, mock payment availability |
| POST /activity/puja | `steps`: LIGHT, BATH, TILAK, FLOWERS, BELL, CONCH, PRASAD, AARTI in that order |
| PUT /shrine | `selections`: seven catalog category keys; requires entitlement (six-key legacy clients are supported) |
| POST /purchase | Authenticated `request_id` (8–128 chars); mock mode grants package and records ₹0 demo payment. Retries are idempotent. Returns 503 with mock mode disabled. |
| POST /logout | `{}`; revokes session |
| DELETE /me | Deletes account, activity and sessions |

The package covers `shrine`, `idols`, `flowers`, `shankh`, `aarti`, `prasad`. The catalog now includes 24 real choices across these categories plus `lamp`. Android provides a thumbnail picker, full-scene preview and server-authorized selection saving. Assets are bundled for offline rendering. See ASSET_PIPELINE.md. Older six-category clients default to the original oil lamp.

## Reward rules (initial product assumptions)

- Base puja stays free without registration.
- Seven consecutive India calendar days unlock the package permanently. Missing a day resets progress, not an earned entitlement.
- One invited account completing its first puja unlocks the inviter's package. Opening the share sheet or registering alone earns nothing. No contact access is requested. Codes are entered on registration and cannot be changed later.
- Completion is deduplicated per authenticated account and server date. Client dates and debug time travel cannot award extra days.
- Android saves one pending completion locally and retries on the same India date via the progress screen. Older offline days are **not** backfilled. Add a trusted offline reconciliation policy before promising offline streak continuity.
- The server validates the reported eight steps; it cannot prove physical/devotional activity. Unverified pilot accounts can still be used to farm referral rewards. Identity verification and abuse controls are required before rewards have monetary value.
- Live Google Play Billing, verified purchase tokens, refunds, restore purchases and pricing are not implemented. No real charge is made. Mock purchase entitlements are granted by the server only with explicit `MOCK_MODE=true`; Android labels them as demo payments. Use a separate demo datastore; never enable this mode for public deployment.

Cached history contains account creation, completed puja days, mock purchases, shrine selections and referral/streak unlocks. The server retains the full history; the sheet shows the latest 20 events. Account deletion removes this history. Failed network requests retain the last successful snapshot; failed purchases are safe to retry with the saved request ID.

## Validation

`go test ./...` covers persistence, authentication, locked selection rejection, step validation, duplicate completion, seven-day unlock, referral qualification, India midnight, missed days and deletion. Android compile and existing unit tests pass. Emulator interaction checks cover mobile login, mock purchase and process restart; TalkBack testing remains pending. No accessibility certification is claimed.

## Remaining launch decisions

Asset delivery and preparation are now implemented; the original checklist below remains a reference. Live store billing, verified identity, hosting and launch policies are still pending. See ASSET_PIPELINE.md for the current asset inventory.

### Original asset brief

1. **Shrines:** clean backgrounds without baked-in idols or offerings; portrait 1440×2560 PNG/WebP, with safe areas and altar placement guides. Provide at least two variants.
2. **Idols:** separate transparent PNG/WebP front-facing images (around 1024px tall), or optimized GLB with textures, consistent scale, origin and orientation. Include names and respectful placement guidance.
3. **Offerings:** independent flower, shankh, lamp/aarti and prasad assets; transparent images or GLB matching the existing renderer. Include small preview thumbnails and usage rights.
4. **Audio and language:** licensed aarti/shankh recordings, lyrics and time markers; reviewed Hindi/English copy and priority regional languages. A ritual reviewer should approve deity-specific sequences and combinations.
5. **Commerce:** package name, one-time price, which variants it includes, reward thresholds, Play Console product ID and provider/backend credentials through secure configuration. Do not commit secrets.
6. **Operations:** HTTPS host, identity provider preference (phone OTP recommended for evaluation), support contact, retention/deletion policy and updated privacy/store disclosures before enabling accounts publicly.
