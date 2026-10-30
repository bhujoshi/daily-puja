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

### Tracked Screens (`screen_view`)
| `screen_name` | `screen_class` | Description |
| :--- | :--- | :--- |
| `mandir_entrance` | `MandirHomeScreen` | Closed temple doors entrance stage |
| `mandir_cleaning` | `MandirHomeScreen` | Morning temple cleaning ritual stage |
| `mandir_sanctum` | `MandirHomeScreen` | Main garbhagriha sanctum & interactive altar |
| `mandir_puja_complete` | `MandirHomeScreen` | Puja completion blessing banner stage |
| `navigation_drawer` | `MandirNavigationDrawer` | Left-side navigation drawer |
| `shrine_customizer` | `ShrineCustomizer` | *Mandir Sajaye* temple customizer sheet |
| `shrine_preview` | `ShrineCustomizer` | Full-screen temple customization preview |
| `shrine_unlock_dialog` | `ShrineCustomizer` | Devotion coin unlock confirmation dialog |
| `mindfulness_setup` | `MindfulnessMeditationScreen` | *Dhyan Sadhana* practice & timer selection screen |
| `mindfulness_active` | `MindfulnessMeditationScreen` | Active breathing, japa, or silent meditation session |
| `mindfulness_completed` | `MindfulnessMeditationScreen` | Meditation completion summary & streak card |
| `music_player` | `BhajanPlayerSheet` | Full 16:9 Bhajan & YouTube video player sheet |
| `music_search` | `BhajanPlayerSheet` | Active Bhajan/YouTube search & autocomplete view |
| `music_player_minimized` | `BhajanPlayerSheet` | Floating side mini-player while browsing catalog |
| `devotion_profile` | `DevotionSheet` | Signed-in Devotion Profile, Streak Calendar & Coin Passbook |
| `devotion_package` | `DevotionSheet` | Guest Devotion Package & Coin Passbook view |
| `devotion_login` | `DevotionSheet` | Google Sign-In prompt sheet |
| `devotion_share` | `DevotionSheet` | Referral & WhatsApp invitation sheet |
| `devotion_delete_confirm` | `DevotionSheet` | Account deletion confirmation view |
| `aarti_player` | `AartiPlayerSheet` | Synced Aarti lyrics sheet |

### Custom & Standard Activity Events
| Event Name | Parameters | Trigger |
| :--- | :--- | :--- |
| `screen_view` | `screen_name`, `screen_class` | Navigating between any screen, stage, modal sheet, or sub-view |
| `app_lifecycle` | `state` (`foreground` \| `background`) | App enters foreground (`ON_START`) or background (`ON_STOP`) |
| `temple_entered` | `needs_cleaning`, `deity_count`, `shrine_id`, `idols_id` | User opens the temple doors |
| `temple_cleaning_started` | `method` (`tap` \| `swipe`) | User begins scrubbing the temple floor |
| `temple_cleaned` | `method` (`tap` \| `swipe`) | User completes the morning temple cleaning ritual |
| `ritual_diya_lit` | `source` (`altar` \| `dock`), `active_step` | User lights the brass diya |
| `ritual_deity_bathed` | `deity_index`, `source`, `active_step` | User performs Abhishek (water bath) on a deity |
| `ritual_tilak_applied` | `deity_index`, `source`, `active_step` | User applies Sindoor/Chandan tilak on a deity |
| `ritual_flower_offered` | `deity_index`, `flower_index`, `source`, `active_step` | User offers a flower garland to a deity |
| `ritual_conch_blown` | `source` (`altar` \| `dock`), `active_step` | User blows the sacred Shankh |
| `ritual_prasad_offered` | `source` (`altar` \| `dock`), `active_step` | User offers Modak/Ladoo prasad |
| `ritual_aarti_started` | `source` (`altar` \| `dock`), `active_step` | User begins the circular Aarti thali offering |
| `ritual_bell_rung` | `source` (`altar` \| `dock`) | User rings the brass temple bell |
| `ritual_step_completed` | `step`, `step_index`, `deity_count` | User completes each puja step (`light`, `bath`, `tilak`, `flowers`, `conch`, `prasad`, `aarti`) |
| `puja_completed` | `deity_count`, `shrine_id`, `idols_id`, `streak_days`, `signed_in` | User finishes the full daily puja & aarti |
| `drawer_item_selected` | `item` (`mini_player`, `profile`, `mandir_sajaye`, `mindfulness`, `music_player`, `whatsapp_share`, `language_toggle`, `debug_pass_day`) | User selects an item in the temple navigation drawer |
| `shrine_category_viewed` | `category_id` | User views or switches categories in *Mandir Sajaye* |
| `shrine_option_selected` | `category_id`, `item_id`, `owned`, `cost_coins`, `selected` | User taps a shrine/murti/lamp/flower option in *Mandir Sajaye* |
| `shrine_preview_opened` | `shrine_id`, `idols_id` | User previews a temple customization |
| `shrine_unlock_prompted` | `item_count`, `total_cost_coins`, `sufficient_coins`, `signed_in` | Unlock confirmation dialog is shown for locked shrine items |
| `shrine_unlock_cancelled` | `item_count`, `total_cost_coins` | User dismisses the unlock confirmation dialog |
| `shrine_item_unlocked` | `category_id`, `item_id`, `cost_coins` | User unlocks a shrine item with devotion coins |
| `shrine_selection_applied` | `shrine_id`, `idols_id`, `lamp_id`, `flower_count` | User applies a new temple customization |
| `shrine_account_clicked` | `signed_in`, `coins` | User taps the Devotion Coins pill in *Mandir Sajaye* |
| `mindfulness_option_selected` | `option_type` (`practice` \| `breathing_pattern` \| `mantra` \| `duration`), `value` | User configures a *Dhyan Sadhana* session option |
| `mindfulness_sound_toggled` | `enabled` | User toggles ambient Om/bell cues in *Dhyan Sadhana* |
| `mindfulness_started` | `practice`, `duration_minutes`, `sub_option` | User begins a *Dhyan Sadhana* meditation session |
| `mindfulness_pause_toggled` | `practice`, `paused`, `elapsed_seconds` | User pauses or resumes an active meditation session |
| `mindfulness_japa_tap` | `mantra`, `japa_count`, `mala_completed` | User taps the interactive Japa bead counter (logged on first bead and every 9th/108th bead) |
| `mindfulness_cancelled` | `practice`, `elapsed_seconds`, `japa_count` | User exits an active meditation session to change practice |
| `mindfulness_completed` | `practice`, `duration_minutes`, `streak_days`, `total_sessions`, `completed_early`, `japa_count` | User completes a meditation session (full timer or early finish) |
| `mindfulness_restarted` | `practice` | User taps *Meditate Again* on the completion card |
| `bhajan_played` | `track_id`, `track_title`, `deity`, `source`, `autoplay` | User or aarti plays a bhajan track |
| `bhajan_search` | `query_length`, `result_count` | User searches for a bhajan |
| `bhajan_control_action` | `action` (`play`, `pause`, `retry`, `skip_next`, `skip_previous`, `seek`, `repeat_on`, `repeat_off`, `remove_player`, `clear_search`, `suggestion_selected`, `suggestion_fill`, `minimize_to_side`, `expand_from_side`, `dock_side_toggle`, `retry_catalog`), `track_id`, `source` | User interacts with Bhajan/YouTube player transport or sheet controls |
| `aarti_lyrics_action` | `action` (`play` \| `pause` \| `complete`), `title` | User interacts with the Aarti lyrics sheet |
| `language_changed` | `language` (`hi` \| `en`) | User toggles language in the temple drawer |
| `google_sign_in_started` | `referred_install` | User taps *Sign in with Google* |
| `google_sign_in_result` | `outcome` (`completed` \| `cancelled` \| `failed`), `reason` | Google sign-in attempt completes or fails |
| `whatsapp_share_opened` | — | User opens WhatsApp to share an invitation |
| `streak_calendar_month_changed` | `year_month`, `direction` (`previous` \| `next`) | User navigates months in the Devotion Streak Calendar |
| `passbook_filter_changed` | `filter` (`all` \| `earned` \| `spent`) | User filters the Devotion Coin Passbook |
| `passbook_expanded_toggled` | `expanded`, `entry_count` | User expands or collapses the Devotion Coin Passbook history |
| `account_action` | `action` (`sign_out`, `delete_account`, `delete_confirm_opened`, `delete_confirm_cancelled`, `refresh_sync`, `view_profile_from_share`, `customize_from_profile`, `customize_from_login`, `share_from_passbook`, `login_prompt_clicked`) | User performs an account or profile navigation action |
