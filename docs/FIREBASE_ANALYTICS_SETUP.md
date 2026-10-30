# Firebase Analytics Setup (`com.pavitramandir.app`)

Firebase Analytics (Google Analytics 4) is integrated into the Android app through [`MandirAnalytics`](../android/app/src/main/java/com/worship/nityamandir/data/MandirAnalytics.kt) in compliance with the Devotional Privacy Guardrails ([`docs/GUARDRAILS.md`](./GUARDRAILS.md) §2.4):

- **No Advertising ID (`AD_ID`) or Ad Personalization**: `com.google.android.gms.permission.AD_ID` is explicitly removed and ad personalization signals are disabled in [`AndroidManifest.xml`](../android/app/src/main/AndroidManifest.xml).
- **Zero PII**: No emails, phone numbers, OAuth tokens, or personal identifiers are ever sent to analytics.
- **Offline & Config Resilient**: Builds and unit tests succeed even before `google-services.json` is added; once `android/app/google-services.json` is present, Gradle automatically applies `com.google.gms.google-services`.

---

## 1. Connect Firebase & Add `google-services.json`

1. Open the **[Firebase Console](https://console.firebase.google.com/)** and click **Add project** (or select your existing Google Cloud project `gen-lang-client-0511734124` / `653836355193`).
2. Enable **Google Analytics** when prompted during project setup.
3. In Project Overview, click **Add app → Android**:
   - **Android package name**: `com.pavitramandir.app`
   - **App nickname**: `Pavitra Mandir`
4. Download **`google-services.json`** and place it at:
   ```text
   android/app/google-services.json
   ```
5. Rebuild the app (`./gradlew :app:assembleDebug` or `./gradlew :app:bundleRelease`). The `com.google.gms.google-services` plugin will automatically detect `android/app/google-services.json` and embed the Firebase resources.

---

## 2. Verify Live Events with Firebase DebugView

To inspect events in real time on a connected Android device or emulator:

```sh
# Enable Firebase Analytics DebugView for Pavitra Mandir
adb shell setprop debug.firebase.analytics.app com.pavitramandir.app
```

Then open **Firebase Console → Analytics → DebugView** while interacting with the app. To disable debug mode later:

```sh
adb shell setprop debug.firebase.analytics.app .none.
```

---

## 3. Tracked Events & User Properties

### User Properties
| Property | Values | Purpose |
| :--- | :--- | :--- |
| `app_language` | `hi`, `en` | Language preference cohort |
| `account_type` | `google`, `guest` | Signed-in vs. guest user cohort |
| `active_idols` | `original`, `ram_darbar`, `shiva`, etc. | Currently selected murti set |

### Custom & Standard Events
| Event Name | Parameters | Trigger |
| :--- | :--- | :--- |
| `screen_view` | `screen_name`, `screen_class` | Navigating between entrance, sanctum, drawer, customizer, music player, and account sheets |
| `temple_entered` | `needs_cleaning`, `deity_count`, `shrine_id`, `idols_id` | User opens the temple doors |
| `temple_cleaned` | `method` (`tap` \| `swipe`) | User completes the morning temple cleaning ritual |
| `ritual_step_completed` | `step`, `step_index`, `deity_count` | User completes each puja step (`light`, `bath`, `tilak`, `flowers`, `conch`, `prasad`, `aarti`) |
| `ritual_bell_rung` | — | User rings the brass temple bell |
| `puja_completed` | `deity_count`, `shrine_id`, `idols_id`, `streak_days`, `signed_in` | User finishes the full daily puja & aarti |
| `shrine_category_viewed` | `category_id` | User switches categories in *Mandir Sajaye* |
| `shrine_preview_opened` | `shrine_id`, `idols_id` | User previews a temple customization |
| `shrine_item_unlocked` | `category_id`, `item_id`, `cost_coins` | User unlocks a shrine item with devotion coins |
| `shrine_selection_applied` | `shrine_id`, `idols_id`, `lamp_id`, `flower_count` | User applies a new temple customization |
| `bhajan_played` | `track_id`, `track_title`, `deity`, `source`, `autoplay` | User or aarti plays a bhajan track |
| `bhajan_search` | `query_length`, `result_count` | User searches for a bhajan |
| `language_changed` | `language` (`hi` \| `en`) | User toggles language in the temple drawer |
| `google_sign_in_started` | `referred_install` | User taps *Sign in with Google* |
| `google_sign_in_result` | `outcome` (`completed` \| `cancelled` \| `failed`), `reason` | Google sign-in attempt completes or fails |
| `whatsapp_share_opened` | — | User opens WhatsApp to share an invitation |
| `account_action` | `action` (`sign_out` \| `delete_account`) | User logs out or deletes their account |
