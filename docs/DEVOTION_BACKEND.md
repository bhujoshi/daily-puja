# Devotion accounts pilot

Implemented for Android and the Go backend; iOS and web remain unchanged. This is a local backend foundation, not a deployed payment or asset store.

## Run and connect

From `backend`, run `MOCK_MODE=true go run ./cmd/server` for the local demo. Accounts persist at `data/accounts.json` (override with `ACCOUNT_DATA_PATH`). The server binds loopback on port 8080; use a TLS reverse proxy for a device connection. Android debug builds default to `http://10.0.2.2:8080`, the emulator's route to the development machine's localhost. Override with `./gradlew assembleDebug -PaccountApiUrl=https://YOUR_API_HOST` for a different server. Puja continues offline when the backend is unavailable.

Storage is an atomic, owner-only JSON file for **one server process**. Back up securely. Migrate to transactional SQL, add monitoring, request throttling at the proxy, verified identity, account recovery and backup deletion policy before public deployment. Android login uses Google Credential Manager; the backend validates signed ID tokens and a single-use challenge. Configure the Web OAuth client ID and Android signing certificates following [GOOGLE_LOGIN_SETUP.md](GOOGLE_LOGIN_SETUP.md). Optional backend OTP routes are documented in [OTP_SETUP.md](OTP_SETUP.md). Sessions expire after 30 days, are hashed on the server and encrypted with Android Keystore on the device. Reopening restores the session and cached profile, including shrine selections, credited days, streak and activity/payment history. Android backup is disabled for this account cache. Refresh fetches server state; a rejected/expired session clears the local login. Do not enable cleartext HTTP for release. Debug builds can connect to `http://10.0.2.2:8080` using the debug-only local network configuration.

Legacy `/api/v1` prototype endpoints trust supplied user IDs and dates. They are disabled by default; `ENABLE_LEGACY_DEMO=true` explicitly enables them for development only. Their old streak state is separate from the account service.

## API

All paths below start `/api/v2`. JSON errors use `error`. Authenticated requests require `Authorization: Bearer TOKEN`.

| Method/path | Body / behavior |
| --- | --- |
| POST /auth/google/challenge | `{}`; returns a short-lived sign-in nonce |
| POST /auth/google | `id_token`, `nonce`, optional `invite_code`, `install_id`; verified Google login returns token/profile |
| POST /auth/otp/request | `phone`; sends WhatsApp/SMS via configured provider (demo notice in mock mode) |
| POST /auth/otp/verify | `phone`, `otp`, optional `invite_code` (`1234` in mock mode); creates or signs into the same mobile account |
| POST /register | `email`, `password` (10–128 chars), optional `invite_code`; returns token/profile |
| POST /login | email/password; returns token/profile |
| GET /catalog | Seven categories, reward thresholds and availability flags |
| GET /me | Mobile, account ID, streak, credited days, invite code, qualified referrals, selections, entitlement, activity/payment history, mock payment availability |
| POST /activity/puja | `steps`: LIGHT, BATH, TILAK, FLOWERS, BELL, CONCH, PRASAD, AARTI in that order |
| PUT /shrine | `selections`: seven catalog category keys; requires ownership of every selected paid item (six-key legacy clients are supported) |
| POST /purchase | Authenticated `request_id` (8–128 chars); mock mode grants package and records ₹0 demo payment. Retries are idempotent. Returns 503 with mock mode disabled. |
| POST /logout | `{}`; revokes session |
| DELETE /me | Deletes account, activity and sessions |

The package covers `shrine`, `idols`, `flowers`, `shankh`, `aarti`, `prasad`. The catalog now includes 24 real choices across these categories plus `lamp`. Android provides a thumbnail picker, full-scene preview and server-authorized selection saving. Assets are bundled for offline rendering. See ASSET_PIPELINE.md. Older six-category clients default to the original oil lamp.

## Coins and WhatsApp referrals

- Completed puja earns **1 coin per account per India calendar day**. All eight steps must be reported in order. Repeats are idempotent, and missing days does not reset the wallet. Server time determines eligibility; debug time travel cannot award coins.
- A **new referred installation followed by Google sign-in earns the inviter 1 coin**. Each new Google subject and app-scoped device identity can qualify once. Multiple friends using the same link each qualify. Opening WhatsApp, a repeat install, an existing account, self-referral, or the friend's first puja does not earn a referral coin.
- Each additional shrine costs **7 coins**; each additional flower variety costs **2 coins**. Original items, shankh, aarti, prasad, idols and oil lamps are free. Idols and oil lamps were unspecified in the request and default to free. Unlocks are permanent, per catalog item, and separate from selection. The server rejects unowned selections and insufficient balances. Repeated unlock requests cannot spend twice.
- Legacy package and owner entitlements remain valid. Existing distinct credited puja days and qualified referrals initialize a wallet once. No new streak or referral grants an all-items package. The old mock-purchase endpoint remains only for existing test clients, and is no longer offered by the Android UI.
- Android requires Google sign-in before creating a share link. Every share gets a random opaque `share_id` mapped to the sender on the server. The URL is a Play Store listing URL with an encoded `referrer=share_id=...`; it contains no email or account ID. WhatsApp is opened directly, with WhatsApp Business as fallback. The user chooses the recipient and sends the message. An absent WhatsApp app produces a recoverable error.
- `share_count` counts successful handoffs to WhatsApp, **not messages delivered**. Share creation and handoff reporting are separately idempotent. Failed handoff reports are queued on the device per account and retried on sync. No coin is awarded for a handoff.
- The app reads [Google Play Install Referrer](https://developer.android.com/google/play/installreferrer/library) on first execution and retains the attribution until a successful claim. A referred guest is offered Google login and may dismiss it to worship offline. The backend binds the claim to the Google account's initial app-scoped installation identity and checks the install time against share creation and account creation. Devices already registered to an account cannot qualify a second account. Claims and deductions use the same mutex and atomic persistence as the wallet.
- This is **attribution with duplicate prevention, not server attestation of a genuine install**. Install Referrer values and the hashed device identity are submitted by the app. A modified client can falsify them. Integrate server-validated Play Integrity verdicts and abuse rate limits before assigning monetary value to coins. Factory resets, other Android users and signing-key changes can change the scoped device identity.
- Account deletion removes wallet, history, selections, owned items, sessions, and outbound share links. Minimal one-way digests of previously registered device identities and rewarded Google subjects remain to prevent account deletion/recreation from earning another reward. These are pseudonymous anti-abuse records, not anonymous data.
- Worship stays available offline. Android keeps one completion pending for retry on the same India date; older offline days are **not backfilled**. A trusted offline reconciliation policy is still required before promising rewards for unsynced earlier days.

### New API routes

All require a valid bearer session. Share creation and install claims additionally require a verified Google account.

| Method/path | Body / response |
| --- | --- |
| POST /coins/unlock | `category`, `item`; deducts the server's catalog price once and returns profile; 409 for insufficient coins |
| POST /shares | `request_id` (8–128 chars); returns stable `share_id`, `url` for retries of the same request |
| POST /shares/opened | `share_id`; records one WhatsApp handoff for the owning account and returns profile |
| POST /referrals/install | `share_id`, `install_id` (SHA-256 of app-scoped Android identity), `installed_at` (Play install Unix seconds); returns `processed`, `credited` |

Google login also accepts `install_id` to bind the first account creation to a registered installation. `GET /me` includes `coins`, `owned` (keys such as `shrine:marble`), `share_count`, `referrals`, `google_account`, and `coin_costs`. `PUT /shrine` validates ownership of every item, including each comma-separated flower. Free variants do not require the legacy package entitlement.

## Validation and release

Backend tests cover daily caps, India midnight, missed days, new and legacy account persistence, coin migration, free variants, unowned selections, concurrent spending, rollback on persistence failure, share idempotency and Google-only authorization, multiple referred users, repeated claims, ineligible existing accounts, self-referral, same-device accounts, deletion/recreation, and install timestamps. Android unit tests and build verification cover compilation and the existing ritual suite.

A live end-to-end attribution test must use the release-signed app installed from a Google Play test track using a real generated referral URL, then Google sign-in. Sideloading an APK does not reproduce Play installation attribution. Test WhatsApp and WhatsApp Business handoffs, missing WhatsApp, cancellation, backend interruption and restart, and wallet restoration on another device. Deploy the updated backend together with the Android build; an older server does not implement these routes. No production deployment is performed by the source change.

## Remaining launch decisions

Asset delivery and preparation are now implemented; the original checklist below remains a reference. Live store billing, verified identity, hosting and launch policies are still pending. See ASSET_PIPELINE.md for the current asset inventory.

### Original asset brief

1. **Shrines:** clean backgrounds without baked-in idols or offerings; portrait 1440×2560 PNG/WebP, with safe areas and altar placement guides. Provide at least two variants.
2. **Idols:** separate transparent PNG/WebP front-facing images (around 1024px tall), or optimized GLB with textures, consistent scale, origin and orientation. Include names and respectful placement guidance.
3. **Offerings:** independent flower, shankh, lamp/aarti and prasad assets; transparent images or GLB matching the existing renderer. Include small preview thumbnails and usage rights.
4. **Audio and language:** licensed aarti/shankh recordings, lyrics and time markers; reviewed Hindi/English copy and priority regional languages. A ritual reviewer should approve deity-specific sequences and combinations.
5. **Commerce:** package name, one-time price, which variants it includes, reward thresholds, Play Console product ID and provider/backend credentials through secure configuration. Do not commit secrets.
6. **Operations:** HTTPS host, Google OAuth project/client configuration, support contact, retention/deletion policy and updated privacy/store disclosures before enabling accounts publicly.
