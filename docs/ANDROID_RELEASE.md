# Android release builds

Application ID: `com.pavitramandir.app`  
Version: `1.0.14` (version code `15`)

## Latest release verification

Version 1.0.14 includes the latest shrine artwork, garland selections, diya lamp
labels (`Single diya` / `Panchmukhi diya`), and ritual layout updates. The
signed APK and AAB, with SHA-256 checksums, are stored in
`android/app/build/outputs/releases/1.0.14/`.

## Version 1.0.13 verification

Version 1.0.13 increments the Android version for this release build. The
versioned APK and AAB, with SHA-256 checksums, are stored in
`android/app/build/outputs/releases/1.0.13/`.

## Version 1.0.12 verification

Version 1.0.12 shortens decoration actions to Preview, Unlock, and Apply. Locked
item cards show a lock and price; tapping the card selects it. The selected total
sits outside the action button, and purchase confirmation retains the full cost
and balance. Guest sign-in still opens only after tapping Unlock.

Verification covers the 17 isolated decoration and main-screen flows, release
lint, and the 53 release unit tests. Signed and development APKs with SHA-256
checksums are stored in `android/app/build/outputs/releases/1.0.12/`.

## Version 1.0.11 verification

Version 1.0.11 keeps priced Unlock actions consistent across guest and signed-in
accounts. Sign-in or coin guidance opens after tapping. WhatsApp actions use the
WhatsApp mark, and the main menu uses a hamburger icon. The compact puja dock
shows the current numbered step and a brief instruction above the existing action.

Verification covers 53 release unit tests, 17 isolated emulator flow checks,
release lint, guest sign-in handoff, step progression, and compact dock bounds
with larger text. Signed and development APKs are stored with SHA-256 checksums in
`android/app/build/outputs/releases/1.0.11/`. No store publication was performed.

## Version 1.0.10 verification

Version 1.0.10 simplifies Mandir Sajaye with a compact preview action bar,
a single-line music player, inline wallet controls, and shorter item rows.
Flowers use visible checkboxes and a selection count, with feedback when trying
to remove the last variety. Opening music preserves the decoration draft and tab.

The release APK uses the existing signing key. All 53 release unit tests, 14
isolated emulator flow tests, and release lint passed. Small-screen checks include
320dp width with 1.3× text, music visible, multiple flowers, and purchase/apply.
The preview action area measures about 108dp and each compact music row 48dp
on the small-screen emulator configuration.

Signed and development APKs with SHA-256 checksums are stored in
`android/app/build/outputs/releases/1.0.10/`. No store publication was performed.

## Version 1.0.9 verification

Version 1.0.9 adds the missing Apply action to the full-screen Mandir Sajaye
preview. Owned items can be applied there; paid items use the existing unlock
confirmation before applying. Both chooser and preview share the same current
selection and saving action. Saving errors remain visible in the preview.

The signed APK was built with the existing release key, and its certificate
matches version 1.0.8. All 53 release unit tests, ten isolated emulator flow tests,
and release lint passed. The real-screen regression also verifies the open main
altar changes, reopening the chooser retains the item, and activity recreation
restores the saved choice. This fix requires no backend change.

The installable update and SHA-256 checksum are stored locally in
`android/app/build/outputs/releases/1.0.9/`. No store publication was performed.

## Version 1.0.8 verification

Version 1.0.8 includes Mandir Sajaye purchase confirmations, preservation of
selected flowers, profile unlock notices, and serialized account updates.
The signed APK and AAB were built with the existing release key. All 53 release
unit tests, seven isolated emulator purchase-flow tests, and release lint passed.
The backend was rebuilt and deployed to the existing staging Sprite after a
checkpoint and account-data backup. Backend race tests and local/public health,
catalog, authentication, and private-metrics checks passed.

Versioned build copies and SHA-256 checksums are stored locally in
`android/app/build/outputs/releases/1.0.8/`.

## Build from the command line

Use JDK 17 and an Android SDK with platform 36 and Build Tools 35.0.0.
The app compiles against and targets API 36 using Android Gradle Plugin 8.10.1.
Configure the SDK location in
`android/local.properties` (`sdk.dir=/your/android/sdk`). On this Mac:

```sh
cd /Users/bhuwanjoshi/work/worship/android
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :app:assembleRelease :app:bundleRelease :app:testReleaseUnitTest
```

Outputs, relative to the `android` directory:

- Signed installable APK: `app/build/outputs/apk/release/app-release.apk`
- Signed bundle for Google Play: `app/build/outputs/bundle/release/app-release.aab`

To build only the APK, run `./gradlew :app:assembleRelease`. To build only the
bundle, run `./gradlew :app:bundleRelease`. A clean build is normally unnecessary;
prepend `clean` to the Gradle tasks if needed.

Debug and release builds use `https://mcp-daily-puja-backend-staging-b3mqb.sprites.app`
as the account API base URL. The app adds `/api/v2/` to request paths.
To override the URL, pass `-PaccountApiUrl=https://your-backend.example` to
Gradle. For a local backend, pass `-PaccountApiUrl=http://10.0.2.2:8080`, the
Android emulator's route to the development machine's localhost, and start the
backend with `GOOGLE_CLIENT_ID` set as described in [GOOGLE_LOGIN_SETUP.md](GOOGLE_LOGIN_SETUP.md).

Google login uses the public Web client ID in `android/gradle.properties`; override
it with `-PgoogleClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` and set
the same `GOOGLE_CLIENT_ID` on the backend. Register the installed build's
signing certificate SHA-1 with Google's Android OAuth client. See
[GOOGLE_LOGIN_SETUP.md](GOOGLE_LOGIN_SETUP.md). Google login was verified on the
emulator with the regular debug package and staging backend. Real payments still require a payment provider.

## Signing key

The release uses `android/keystores/pavitramandir-release.jks`, alias
`pavitramandir-release`. Its generated passwords and location are stored in
`android/keystore.properties`. Both files are excluded from Git and have
owner-only file permissions.

Back up both files securely, preferably in an encrypted backup or password
manager. Keep the same key for subsequent releases; do not regenerate it for
each build. Do not share the credentials or commit them to source control.

On another computer, restore both files at the same relative paths. The local
properties file has this structure (placeholders below are not real passwords):

```properties
storeFile=keystores/pavitramandir-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=pavitramandir-release
keyPassword=YOUR_KEY_PASSWORD
```

Release builds fail if the signing properties file is absent. Debug builds do
not require it.

Before publishing a later release, increase `versionCode` in
`android/app/build.gradle.kts` and update `versionName` as appropriate. The bundle
is a build artifact; store listing, Play App Signing setup, and Play Console
policy checks are separate publishing steps.
