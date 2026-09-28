# Google login setup

Android now uses Credential Manager's Google sign-in button instead of phone OTP.
The Go backend verifies Google's signature, issuer, audience, expiry and verified
email with Google's official `idtoken` library. It also checks a server-issued,
five-minute, single-use nonce before creating a normal 30-day app session.
Firebase Authentication and SMS/WhatsApp accounts are not required for this flow.

## Google Cloud setup

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create or
   select a project for Pavitra Mandir.
2. Configure **Google Auth Platform**: branding/app name, support email and
   developer contact. Choose **External** audience for general Google accounts.
   During testing add your Google account under test users. Request only basic
   sign-in identity, email and profile information.
3. Under **Clients**, create an OAuth client of type **Android**:
   - Package: `com.pavitramandir.app`.
   - SHA-1: the certificate fingerprint of the build being installed.
   - Create separate Android clients for debug, directly installed release, and
     Google Play app signing when those certificates differ, in the same project.
   - Get debug and local release fingerprints with `./gradlew :app:signingReport`
     from `android`. For Play installs, copy the **App signing key certificate**
     SHA-1 from Play Console → App integrity (the upload key is different).
4. Create another OAuth client of type **Web application** in the same project.
   This flow needs its **client ID** ending in `.apps.googleusercontent.com`.
   No redirect URI, client secret or service account key is used by this Android
   ID-token flow. Do not use the Android client ID as the backend audience.

References: [Google Auth Platform prerequisites](https://developer.android.com/identity/sign-in/credential-manager-siwg)
and [Credential Manager implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).

### Signing fingerprints on this Mac

Verified with `:app:signingReport`:

| Installed build | SHA-1 |
| --- | --- |
| Debug | `9B:DE:BC:AC:B7:99:F4:C6:25:53:37:D2:55:2B:0A:CD:8C:CB:F5:61` |
| Directly installed release | `5D:9B:97:33:C1:36:38:44:F4:DA:B9:EE:A4:5F:66:10:94:90:C4:0F` |

For Google Play installs, use Play Console's app signing fingerprint instead.

## Configure the backend

In the deployment's private `backend/deploy/.env`, set:

```dotenv
GOOGLE_CLIENT_ID=653836355193-vvbqtr7f50p3ct29c3id30pt8doak8is.apps.googleusercontent.com
MOCK_MODE=false
OTP_PROVIDER=
```

Keep the existing Grafana settings. From `backend/deploy`:

```sh
docker compose up -d --build api
```

For local Go development, export `GOOGLE_CLIENT_ID` before `go run ./cmd/server`.
The Go server does not load `.env` automatically. An unset client ID leaves Google
login disabled; a malformed client ID fails startup. Outbound HTTPS access to
Google's public signing-key endpoint is required.

## Configure Android

Use the same Web client ID as the `googleClientId` Gradle property. It is a public
identifier, not a secret. You can persist it in your local `~/.gradle/gradle.properties`
or pass it when building:

```sh
./gradlew :app:assembleDebug \
  -PgoogleClientId=653836355193-vvbqtr7f50p3ct29c3id30pt8doak8is.apps.googleusercontent.com \
  -PaccountApiUrl=http://10.0.2.2:8080
```

For a release, use the same property with `:app:assembleRelease` or
`:app:bundleRelease` and the HTTPS backend URL. The project now supplies this public client ID in `android/gradle.properties`;
`-PgoogleClientId` can override it. Existing encrypted app sessions
still restore normally.

## Test the flow

Use a device or emulator with Google Play services. Open the profile/login sheet,
optionally enter an invitation code, tap **Sign in with Google**, select an account
and confirm consent. Cancelling returns to the login sheet. Reopen the app to
check session restoration; log out to choose another Google account.

API flow:

- `POST /api/v2/auth/google/challenge` with `{}` returns `nonce` and a 300-second lifetime.
- Pass this nonce to Credential Manager's sign-in request.
- `POST /api/v2/auth/google` with `id_token`, `nonce` and optional `invite_code`
  returns the normal `token` and `profile` after verification.
- Invalid tokens, nonce mismatch, expired challenges and replay return 401;
  throttling returns 429; unconfigured Google login returns 503.

Accounts are keyed by Google's stable `sub`, not email. Existing phone/password
accounts are not automatically merged with Google accounts. Their sessions remain
valid until expiry/logout; merging needs a separate authenticated linking flow.
Challenges and throttling are per process and reset on restart; the existing
account store still supports one server process.

Troubleshooting: verify package, installed signing certificate SHA-1, test-user
access, Google Play services, and that the Web client ID matches on Android and
backend. The backend does not log Google tokens. Account deletion removes the app
account and app sessions; it does not delete the user's Google account.

Backend tests use signed test JWTs with a stub Google key endpoint and cover
signature/claim rejection, expiry, nonce mismatch/replay, repeat login, persistence
and deletion. Actual Google sign-in requires the configured OAuth project and a
real device account.

## Configured staging deployment

The supplied Web client ID is saved in `android/gradle.properties` and the Compose
configuration. The staging Sprites backend at
`https://mcp-daily-puja-backend-staging-b3mqb.sprites.app` has been updated with the
same ID, `MOCK_MODE=false` and OTP delivery disabled. Checkpoint `v1` was created
before deployment. Backend tests passed on staging; public health and Google
challenge returned 200, and an invalid ID token returned 401.

The debug APK at `android/app/build/outputs/apk/debug/app-debug.apk` includes the
staging URL and this Web client ID. Android build and unit tests passed. A real
Google account sign-in still needs an on-device test and matching Android OAuth
certificate/test-user configuration in Google Cloud.
