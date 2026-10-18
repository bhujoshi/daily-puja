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
| Google Play release (Pixel 9, version 1.0.15, verified 2026-10-18) | `6C:85:7B:BE:24:50:44:AE:E3:64:A4:C4:8B:B6:AA:8D:B8:C6:D1:6F` |

For Google Play installs, use Play Console's app signing fingerprint instead.
The Play fingerprint above was extracted from the installed APK and differs
from the direct-release/upload certificate. Register it as a separate Android
OAuth client in the same project as the Web client. On 2026-10-18, inspection
found only debug and direct-release registrations; the Play registration was
missing. Do not replace the Web client ID or the existing Android registrations
to fix this mismatch. Confirm sign-in on a Play-installed build before declaring
a Play release verified; a sideloaded APK does not test Play's signing identity.

With user approval on 2026-10-18, created **Pavitra Mandir — Google Play** for
the verified Play SHA-1 above. Google Cloud confirmed creation of Android client
`653836355193-7nrhj1p08thlcoi3sd60fnb89f4dretg.apps.googleusercontent.com`.
The existing Web client and Android clients were preserved. Google advises
5 minutes to a few hours for propagation. End-to-end sign-in on the Play build
is pending retry after propagation. The creation dialog also reports OAuth
access restricted to consent-screen test users; this registration does not
change the project's audience/publishing status.

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

For local Go development, start the server with:

```sh
GOOGLE_CLIENT_ID=653836355193-vvbqtr7f50p3ct29c3id30pt8doak8is.apps.googleusercontent.com MOCK_MODE=false go run ./cmd/server
```

The Go server does not load `.env` automatically. An unset client ID leaves Google
login disabled; a malformed client ID fails startup. Outbound HTTPS access to
Google's public signing-key endpoint is required.

## Configure Android

Debug and release builds default to the Google-enabled staging backend. A normal
`./gradlew :app:installDebug` can test Google sign-in without a local server.
Use the regular `com.pavitramandir.app` package: `-PisolatedVerification=true`
changes the package to `com.pavitramandir.app.verification`, which requires its
own Android OAuth client before it can be used for Google sign-in.

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
tap **Sign in with Google**, select an account
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
Google account sign-in was verified on the emulator on 2026-10-13 using the
regular debug package and staging backend. The older verification install used
a local backend without `GOOGLE_CLIENT_ID`, which returned 503 before the account
picker opened. Keep matching Android OAuth certificate/test-user configuration
in Google Cloud for each installed package and certificate.

### Signed APK verified on Pixel 9 (2026-10-14)

The real Pixel 9 reproduced the release sign-in failure: the account picker
opened, but selecting an account returned Google Play services status
`UNREGISTERED_ON_API_CONSOLE`. Credential Manager wrapped this provider failure
as `TYPE_USER_CANCELED`; no ID token reached the backend.

Direct inspection of Google Cloud project `653836355193`
(`gen-lang-client-0511734124`) established the cause. Its existing Android client
registered `com.pavitramandir.app` with the **debug** SHA-1 only:
`9B:DE:BC:AC:B7:99:F4:C6:25:53:37:D2:55:2B:0A:CD:8C:CB:F5:61`.
The signed APK's verified SHA-1 is
`5D:9B:97:33:C1:36:38:44:F4:DA:B9:EE:A4:5F:66:10:94:90:C4:0F`.

With user approval, a separate Android client named
**Pavitra Mandir — direct release** was created for the release certificate:
`653836355193-e60oej5pi1rfhrdmecqraavsm8lba0bb.apps.googleusercontent.com`.
The existing debug registration and Web client were preserved. The Web client
ID embedded in the app and configured on the backend remains unchanged.

After registration, the signed release successfully completed Google sign-in
on the Pixel 9 and opened WhatsApp's recipient chooser from the guest sharing
flow. Cancellation returned to a focused sheet with a neutral retry message.
App restart restored the signed-in session; logout and a new sign-in succeeded.
Rapid repeated sign-in taps opened one picker. Sharing records an accepted
WhatsApp handoff, not message delivery; no test message was sent.

The updated app focuses sign-in and sharing sheets on their next action, keeps
coin/pricing/history details in profile and unlock screens, localizes common
provider/network/missing-WhatsApp failures, preserves coroutine cancellation,
and permits a requested WhatsApp handoff before optional progress sync.
Credential fallback is limited to an explicit unsupported-provider exception;
other provider/configuration failures retain their diagnostic classification.

Verification: signed release build and 46 unit tests passed. Four instrumentation
tests passed on the Pixel using a separate verification package and captured
intents: WhatsApp payload, Business fallback, missing-app handling, and Activity
context flags/wrapper safety. See [phone test record](PHONE_SIGNIN_SHARE_TEST.md).
Google Play installs still require their own Play app signing certificate
registration when it differs from the directly installed APK.
