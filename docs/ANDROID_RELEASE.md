# Android release builds

Application ID: `com.pavitramandir.app`  
Version: `1.0.7` (version code `8`)

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
