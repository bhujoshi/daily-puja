# Google sign-in and WhatsApp sharing — phone verification

Date: 2026-10-14 (India time). Device: USB-connected Pixel 9.
Build: directly installed signed release `1.0.7`, version code `8`,
package `com.pavitramandir.app`, Google-enabled staging backend.

## Diagnosis and fix

Google returned `UNREGISTERED_ON_API_CONSOLE` after account selection. Google
Cloud had registered the debug certificate only. A separate release Android
OAuth client was added with user approval, using the APK's verified SHA-1
`5D:9B:97:33:C1:36:38:44:F4:DA:B9:EE:A4:5F:66:10:94:90:C4:0F`.
Successful Google authentication was then observed on the same signed APK.

## Verified behavior

- Guest Share opens a focused Google sign-in sheet with the WhatsApp next step.
- Cancelling Google returns to the same sheet, with a neutral retry message.
- Completing Google sign-in from Share opens WhatsApp's recipient chooser.
- Returning from WhatsApp leaves a focused sharing screen and a handoff notice.
- Profile shows the accepted handoff count, with no coin awarded for handoff.
- Force-stop/relaunch restores the encrypted account session and profile.
- Logout clears the account; signing in again opens the picker and succeeds.
- Rapid repeated sign-in taps open one account picker. Rapid repeated sharing
  taps open one WhatsApp chooser; one Back press returns to the app.
- Hindi and English sign-in/share layouts were inspected on the phone.
- No message was sent and no account was deleted.

## Automated verification

- Signed release assembly and release lint: passed.
- Android release unit tests: 46 passed, 0 failures/errors.
- WhatsApp instrumentation tests on Pixel 9: 4 passed. These capture intents
  instead of changing or launching installed apps and cover text payload,
  WhatsApp Business fallback, missing-app errors, Activity context task flags,
  and cyclic wrapper safety.

The test runner used `com.pavitramandir.app.verification`, separate from the
signed app. The testing stay-awake setting and language preference were restored
after validation. Installed test packages were removed.

This record covers the directly installed signed release. Registration for
Google Play signing uses the certificate from Play Console when different.
