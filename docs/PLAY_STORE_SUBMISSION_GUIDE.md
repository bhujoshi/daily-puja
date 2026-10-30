# Pavitra Mandir (पवित्र मंदिर) — Google Play Store & Fastlane Automation Guide

**Application ID**: `com.pavitramandir.app`  
**Current Version**: `1.0.17` (`versionCode 18`)  
**Supported Store Locales**: `en-US` (English – US), `en-IN` (English – India), `hi-IN` (Hindi – भारत)

---

## 1. Quickstart: Automated Publishing with Fastlane (`supply`)

Everything in `android/fastlane/` is pre-configured with bilingual store listings, release notes (`18.txt`), high-res icons, feature graphics, and 24 screenshots per locale (Phone, 7-inch Tablet, and 10-inch Tablet).

### Step A — Install Fastlane (One-Time Setup)
```bash
cd /Users/bhuwanjoshi/work/worship/android
bundle install
```
*(Or install via Homebrew: `brew install fastlane`)*

### Step B — Create & Invite Your Google Cloud Service Account (New Play Console Flow)

*(Google removed the old "Setup → API access" page from Play Console. Service accounts are now added via **Users and permissions → Invite new users** using the service account's `@...iam.gserviceaccount.com` email address.)*

1. **Enable the Google Play Android Developer API** in your `pavitra-mandir` Google Cloud project:
   - Open: [Google Play Android Developer API (`pavitra-mandir`)](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com?project=pavitra-mandir)
   - Click **Enable**.
2. **Create a Service Account & Download the JSON Key**:
   - Open: [Google Cloud Service Accounts (`pavitra-mandir`)](https://console.cloud.google.com/iam-admin/serviceaccounts?project=pavitra-mandir)
   - Click **+ Create service account**:
     - **Service account name**: `fastlane-play-store`
     - Click **Create and continue** → **Done** (no Cloud IAM role is required; permissions are granted inside Play Console).
   - Copy the generated **Service account email** (looks like `fastlane-play-store@pavitra-mandir.iam.gserviceaccount.com`).
   - Click on the newly created service account → go to the **Keys** tab → click **Add key** → **Create new key** → choose **JSON** → **Create**.
   - Move/rename the downloaded JSON file to:
     ```text
     /Users/bhuwanjoshi/work/worship/android/fastlane/play-service-account.json
     ```
     *(This path is already git-ignored.)*
3. **Grant Access in Google Play Console → Users and permissions**:
   - In **Google Play Console**, go to **Users and permissions** (where you see *Users*, *Invitations*, and *Permission groups*).
   - Click the blue **Invite new users** button on the top-right.
   - In **Email address**, paste your service account email (`fastlane-play-store@pavitra-mandir.iam.gserviceaccount.com`). Leave *Set access expiry date* unchecked.
   - Under **Permissions**, either:
     - Switch to the **Account permissions** tab and check **Admin (all permissions)** *(simplest)*, **OR**
     - Under **App permissions**, click **Add app** → select **Pavitra Mandir (`com.pavitramandir.app`)** → **Apply** → check **View app information (read-only)**, **Manage store presence**, and all **Releases** permissions (*Release to production*, *Release to testing tracks*) → **Apply**.
   - Click **Invite user** → confirm **Send invite** (Service accounts accept automatically within ~1–2 minutes; no email confirmation is needed).

### Step C — Run the Fastlane Command You Need
From `/Users/bhuwanjoshi/work/worship/android`:

| Goal | Command |
| :--- | :--- |
| **1. Validate all character limits, PNG headers & dimensions offline** | `python3 ../scripts/verify_play_store_assets.py` *(or `bundle exec fastlane android validate_metadata`)* |
| **2. Sync ONLY text metadata (`title`, `short_description`, `full_description`, `changelogs`)** | `bundle exec fastlane android sync_metadata` |
| **3. Sync ONLY graphics (`icon`, `featureGraphic`, Phone + 7" + 10" screenshots)** | `bundle exec fastlane android sync_graphics` |
| **4. Sync ALL store listing text + changelogs + all graphics & screenshots (no AAB upload)** | `bundle exec fastlane android sync_all_listing` |
| **5. Upload signed `1.0.17` AAB + full store listing + screenshots to Internal Testing** | `bundle exec fastlane android internal` |
| **6. Upload signed `1.0.17` AAB + full store listing + screenshots to Closed Testing (Alpha)** | `bundle exec fastlane android alpha` |
| **7. Upload signed `1.0.17` AAB + full store listing + screenshots to Open Testing (Beta)** | `bundle exec fastlane android beta` |
| **8. Upload signed `1.0.17` AAB + full store listing + screenshots to Production** | `bundle exec fastlane android production` |

> [!TIP]
> If your Google Play application is still in **Draft** state on Play Console (before its first full review), prefix your command with `PLAY_RELEASE_STATUS=draft`, e.g.:
> ```bash
> PLAY_RELEASE_STATUS=draft bundle exec fastlane android internal
> ```

---

## 2. Fastlane Directory Structure & Generated Assets

All Fastlane metadata is located under [`android/fastlane/`](file:///Users/bhuwanjoshi/work/worship/android/fastlane) and mirrored in [`play_store/`](file:///Users/bhuwanjoshi/work/worship/play_store):

- **Fastlane Config**:
  - [`android/Gemfile`](file:///Users/bhuwanjoshi/work/worship/android/Gemfile)
  - [`android/fastlane/Appfile`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/Appfile)
  - [`android/fastlane/Fastfile`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/Fastfile)
- **Locale Metadata (`en-US`, `en-IN`, `hi-IN`)**:
  - [`android/fastlane/metadata/android/en-US/`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-US)
  - [`android/fastlane/metadata/android/en-IN/`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-IN)
  - [`android/fastlane/metadata/android/hi-IN/`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/hi-IN)
- **Visual Assets Generator & Validator**:
  - [`scripts/GeneratePlayStoreGraphics.java`](file:///Users/bhuwanjoshi/work/worship/scripts/GeneratePlayStoreGraphics.java) — Deterministically renders all 26 PNG assets from the `1.0.17` shrine artwork.
  - [`scripts/verify_play_store_assets.py`](file:///Users/bhuwanjoshi/work/worship/scripts/verify_play_store_assets.py) — Verifies character counts, PNG magic bytes, color types, and exact pixel dimensions.

### Generated Graphics Summary (Synced to `play_store/` & All 3 Fastlane Locales)

| Asset Type | Dimensions | Format | Preview Path |
| :--- | :--- | :--- | :--- |
| **Hi-Res App Icon** | `512 × 512 px` | 32-bit PNG | [`play_store/icon_512x512.png`](file:///Users/bhuwanjoshi/work/worship/play_store/icon_512x512.png) |
| **Feature Graphic** | `1024 × 500 px` | 24-bit RGB PNG | [`play_store/feature_graphic_1024x500.png`](file:///Users/bhuwanjoshi/work/worship/play_store/feature_graphic_1024x500.png) |
| **Phone Screenshots (8)** | `1080 × 2280 px` | 24-bit RGB PNG | [`play_store/screenshots/`](file:///Users/bhuwanjoshi/work/worship/play_store/screenshots) |
| **7-Inch Tablet Screenshots (8)** | `1200 × 1920 px` | 24-bit RGB PNG | [`play_store/screenshots_7inch_tablet/`](file:///Users/bhuwanjoshi/work/worship/play_store/screenshots_7inch_tablet) |
| **10-Inch Tablet Screenshots (8)** | `1600 × 2560 px` | 24-bit RGB PNG | [`play_store/screenshots_10inch_tablet/`](file:///Users/bhuwanjoshi/work/worship/play_store/screenshots_10inch_tablet) |

#### The 8 Screenshot Scenes (`1.0.17` Artwork):
1. **`01_sacred_gateway.png`** — *पवित्र मंदिर में पधारें • Step Inside Your Sacred Sanctum* (Carved Teakwood Temple Doors & Hanging Brass Bells)
2. **`02_swarna_sanctum_puja.png`** — *दिव्य स्वर्ण गर्भगृह दर्शन • Golden Sanctum & Daily Puja* (Shri Ganesha & Maha Lakshmi, Panchmukhi Diya & Kamal Dhoop)
3. **`03_radha_krishna_marble.png`** — *८-चरणीय नित्य पूजा विधि • 8-Step Guided Vedic Rituals* (Makrana Marble Shrine, Radha-Krishna Kalash Abhishek, Tilak & Pushparpan)
4. **`04_aarti_shankh_naad.png`** — *आरती एवं ४३२ Hz शंखनाद • Sacred Aarti & 432 Hz Shankh* (Carved Stone Sanctuary, Shri Ram Darbar, Nagkanya Panch-Aarti & Conch Resonance)
5. **`05_mandir_sajaye_customizer.png`** — *मंदिर सजाएँ • ४ मंदिर व ८ देवता • Customize 4 Shrines & 8 Deities* (Ivory Alcove, Lord Shiva & Mandir Sajaye Customizer Sheet)
6. **`06_bhajan_aarti_player.png`** — *भक्ति भजन एवं आरती संग्रह • Devotional Bhajan & Aarti Player* (Maa Durga, Bilingual Search, Deity Filters & Queue Sheet)
7. **`07_punya_coin_passbook.png`** — *साधना निरंतरता व पुण्य मुद्रा • Devotion Streak & Punya Passbook* (7-Day Streak, Punya Coin Passbook & WhatsApp Family Invite)
8. **`08_morning_shodhan.png`** — *प्रातः मंदिर शुद्धि एवं सफाई • Morning Temple Shodhan* (Clearing Overnight Withered Nirmalya Flowers & Altar Purification)

---

## 3. Store Listing Copy (Ready for Manual Copy-Paste or Fastlane Sync)

### A. English (`en-US` / `en-IN`)

- **App Title** (`27 / 30` chars) — [`title.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-US/title.txt):
  ```text
  Pavitra Mandir - Daily Puja
  ```

- **Short Description** (`77 / 80` chars) — [`short_description.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-US/short_description.txt):
  ```text
  Sacred virtual Hindu temple for daily puja, aarti, shankh, bhajans & darshan.
  ```

- **Full Description** (`2,653 / 4,000` chars) — [`full_description.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-US/full_description.txt):
  ```text
  Pavitra Mandir (पवित्र मंदिर) is a serene, authentic virtual Hindu temple crafted for your daily spiritual practice (Nitya Puja). Whether you are commuting, traveling, studying away from home, or seeking a peaceful moment of darshan, step inside your personal Garbhagriha and perform complete Vedic rituals with tactile reverence.

  ✨ 4 ARCHITECTURAL SHRINES & 8 REVERED DEITIES
  Customize your personal sanctum through Mandir Sajaye (मंदिर सजाएँ):
  • 4 Sacred Shrines: Makrana Marble Arch (संगमरमर मंदिर), Ivory Alcove (हाथीदाँत मंदिर), Carved Stone Sanctuary (नक्काशीदार मंदिर), and Golden Sanctum (स्वर्ण गर्भगृह मंदिर).
  • 8 Divine Murtis: Shri Ganesha & Maha Lakshmi, Ganesha & Hanuman, Radha & Krishna, Lord Shiva, Goddess Lakshmi, Maa Durga, Ram Darbar, and Lord Hanuman.

  🪔 AUTHENTIC 8-STEP DAILY PUJA (अष्टपदी नित्य पूजा विधि)
  Follow a guided step-by-step daily worship flow designed for all ages:
  1. Temple Gateway (मंदिर के पट): Part the carved teakwood doors and ring the hanging brass entrance bells.
  2. Diya & Dhoop (दीप एवं धूप): Light Single Ghee Diya, Panchmukhi Diya, or Mayur Deepstambh alongside Agarbatti, Loban, Royal Kamal Dhoop, or Mayur Dhoop with natural rising smoke.
  3. Dev Snan & Abhishek (देव स्नान): Bathe the deity with sacred water from a traditional copper Kalash.
  4. Sacred Tilak (तिलक धारण): Apply Kumkum, Chandan, Bhasma, or Haldi tilak to the deity's forehead.
  5. Pushparpan (पुष्पांजलि): Adorn the shrine with Marigold (Genda), Rose, Jasmine Mogra, Lotus, Orchid, Lily, or Toran garlands and shower fresh petals.
  6. Ghanti & Aarti (घंटी एवं आरती): Perform circular Aarti with Traditional, Golden, or Nagkanya Panch-Aarti thalis accompanied by resonant temple bells.
  7. Shankh Naad (शंख नाद): Blow the sacred Panchajanya or Swarna Shankh tuned to 432 Hz, resting on a sculpted brass tripod stand.
  8. Bhog & Prasad (भोग समर्पण): Offer Motichoor Laddu, Sooji Halwa, Tirupati Laddu, or Kesar Modak to conclude your puja.

  🎵 DEVOTIONAL BHAJAN & AARTI PLAYER (भजन संगीत)
  • Listen to peaceful Aartis, Chalisas, and Bhajans curated for each deity.
  • Instant bilingual search (Hindi & English), queue management, repeat modes, and local audio caching for uninterrupted devotion.

  🪙 DEVOTION STREAK & PUNYA COIN PASSBOOK (पुण्य मुद्रा पासबुक)
  • Build a mindful daily worship habit with your Nitya Upasana streak counter.
  • Earn Punya Coins through daily puja milestones and by inviting family & friends via WhatsApp.
  • View every reward and unlocked shrine ornament in a transparent Punya Coin Passbook, with optional Google Sign-In to back up your customized mandir across devices.

  🧹 MORNING TEMPLE SHODHAN (मंदिर शुद्धि एवं सफाई)
  • Just like a home temple, yesterday's flower offerings gently age overnight into Nirmalya. Begin your morning by clearing dried petals, wiping the altar clean, and lighting a fresh lamp.

  🙏 DESIGNED FOR PEACE & PRIVACY
  • 100% Ad-Free Sanctuary: No banner ads or jarring popups during worship.
  • Bilingual by Design: Switch effortlessly between Hindi (हिंदी) and English at any time.
  • Senior-Friendly Controls: High-contrast typography, large touch targets, and gentle haptic feedback.

  Bring the serenity of a sacred Hindu temple to your fingertips. Download Pavitra Mandir and begin your daily puja today.
  ```

- **Release Notes (`1.0.17` / `versionCode 18`, `417 / 500` chars)** — [`changelogs/18.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/en-US/changelogs/18.txt):
  ```text
  • New Golden Sanctum, Carved Sanctuary, Ivory Alcove & Marble Arch shrines with 8 revered deities.
  • Expanded Dhoop & Diya catalog including Royal Kamal, Mayur Dhoop, Loban & Panchmukhi lamps.
  • Punya Coin Passbook with transparent transaction history, daily streak tracking & WhatsApp invite rewards.
  • Enhanced Bhajan & Aarti player with instant search, deity filters, queue controls & offline caching.
  • Full-screen edge-to-edge UI polish and smoother ritual transitions.
  ```

---

### B. Hindi (`hi-IN`)

- **App Title** (`25 / 30` chars) — [`title.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/hi-IN/title.txt):
  ```text
  पवित्र मंदिर - दैनिक पूजा
  ```

- **Short Description** (`75 / 80` chars) — [`short_description.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/hi-IN/short_description.txt):
  ```text
  दैनिक पूजा, आरती, शंखनाद, भजन और दिव्य दर्शन के लिए आपका पावन डिजिटल मंदिर।
  ```

- **Full Description** (`2,175 / 4,000` chars) — [`full_description.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/hi-IN/full_description.txt):
  ```text
  पवित्र मंदिर (Pavitra Mandir) आपकी दैनिक आध्यात्मिक साधना और नित्य पूजा के लिए बनाया गया एक शांत, प्रामाणिक और दिव्य डिजिटल हिंदू मंदिर है। चाहे आप यात्रा में हों, घर से दूर हों, या दिन की शुरुआत ईश्वर के पावन दर्शन से करना चाहते हों — अपने व्यक्तिगत गर्भगृह में प्रवेश करें और श्रद्धापूर्वक संपूर्ण वैदिक पूजा संपन्न करें।

  ✨ ४ भव्य मंदिर स्वरूप और ८ पूज्य देवी-देवता
  'मंदिर सजाएँ' के माध्यम से अपने निजी गर्भगृह को अपनी पसंद के अनुसार अलंकृत करें:
  • ४ पावन मंदिर: मकराना संगमरमर मंदिर, हाथीदाँत रंग का मंदिर, नक्काशीदार पाषाण मंदिर और भव्य स्वर्ण गर्भगृह मंदिर।
  • ८ दिव्य मूर्तियाँ: श्री गणेश एवं महालक्ष्मी, श्री गणेश एवं हनुमान जी, राधा-कृष्ण, भगवान शिव, माँ लक्ष्मी, माँ दुर्गा, श्री राम दरबार और संकटमोचन हनुमान जी।

  🪔 अष्टपदी नित्य पूजा विधि (८-चरणीय दैनिक पूजा)
  सभी आयु वर्ग के भक्तों के लिए सरल और मार्गदर्शित पूजा अनुभव:
  १. मंदिर के पट: नक्काशीदार सागौन के द्वार खोलें और प्रवेश द्वार की पीतल की घंटी बजाएँ।
  २. दीप एवं धूप प्रज्वलन: एकल घी दीपक, पंचमुखी दीप या मयूर दीपस्तंभ जलाएँ तथा अगरबत्ती, लोबान, शाही कमल धूप या मयूर धूप अर्पित करें।
  ३. देव स्नान एवं अभिषेक: तांबे के कलश से पवित्र जल द्वारा भगवान का अभिषेक करें।
  ४. तिलक धारण: मस्तक पर कुमकुम, चंदन, भस्म या हल्दी का पावन तिलक लगाएँ।
  ५. पुष्पांजलि एवं माला: गेंदा, गुलाब, मोगरा, कमल, ऑर्किड, लिली या तोरण की माला पहनाएँ और ताज़ी पंखुड़ियाँ अर्पित करें।
  ६. घंटी एवं आरती: पारंपरिक, स्वर्ण या नागकन्या पंच-आरती थाली से मधुर घंटा-ध्वनि के साथ आरती उतारें।
  ७. शंख नाद: ४३२ हर्ट्ज़ (432 Hz) की आध्यात्मिक आवृत्ति में पीतल के स्टैंड पर रखे पाञ्चजन्य या स्वर्ण शंख का नाद करें।
  ८. भोग समर्पण: मोतीचूर लड्डू, सूजी हलवा, तिरुपति लड्डू या केसर मोदक का नैवेद्य अर्पित कर पूजा पूर्ण करें।

  🎵 भक्ति भजन एवं आरती प्लेयर (भजन संगीत)
  • प्रत्येक देवी-देवता के लिए चयनित मधुर आरती, चालीसा और भजनों का आनंद लें।
  • हिंदी और अंग्रेज़ी में त्वरित खोज, कतार (Queue) प्रबंधन, रिपीट मोड और ऑफ़लाइन कैशिंग की सुविधा।

  🪙 साधना निरंतरता (Streak) एवं पुण्य मुद्रा पासबुक
  • अपनी दैनिक पूजा की निरंतरता बनाए रखें और हर दिन पुण्य मुद्रा (Punya Coins) अर्जित करें।
  • व्हाट्सऐप के माध्यम से परिवार और मित्रों को आमंत्रित कर अतिरिक्त पुण्य मुद्रा प्राप्त करें।
  • पारदर्शी पासबुक में अपने सभी पुरस्कार और अनलॉक किए गए मंदिर अलंकरण देखें, तथा वैकल्पिक Google साइन-इन से अपने मंदिर को सुरक्षित रखें।

  🧹 प्रातः मंदिर शुद्धि (निर्माल्य विसर्जन)
  • घर के मंदिर की तरह ही पिछले दिन के फूल अगले दिन निर्माल्य बन जाते हैं। सुबह सूखे फूलों को हटाकर और वेदी को स्वच्छ कर नई ऊर्जा के साथ पूजा आरंभ करें।

  🙏 शांति, पवित्रता और गोपनीयता
  • १००% विज्ञापन-मुक्त अनुभव: पूजा के दौरान कोई विज्ञापन या व्यवधान नहीं।
  • द्विभाषी सुविधा: किसी भी समय हिंदी और अंग्रेज़ी भाषा के बीच आसानी से बदलें।
  • वरिष्ठ नागरिकों के लिए अनुकूल: स्पष्ट अक्षर, बड़े बटन और सौम्य स्पर्श अनुभव।

  आज ही 'पवित्र मंदिर' डाउनलोड करें और अपने फ़ोन पर नित्य पूजा एवं दिव्य दर्शन का आनंद लें।
  ```

- **Release Notes (`1.0.17` / `versionCode 18`, `343 / 500` chars)** — [`changelogs/18.txt`](file:///Users/bhuwanjoshi/work/worship/android/fastlane/metadata/android/hi-IN/changelogs/18.txt):
  ```text
  • ४ भव्य मंदिर (स्वर्ण गर्भगृह, नक्काशीदार, हाथीदाँत व संगमरमर) और ८ पूज्य देवी-देवताओं के दिव्य दर्शन।
  • शाही कमल धूप, मयूर धूप, लोबान और पंचमुखी दीप के साथ विस्तृत पूजा सामग्री।
  • नई पुण्य मुद्रा पासबुक, दैनिक साधना स्ट्रीक और व्हाट्सऐप परिवार आमंत्रण पुरस्कार।
  • त्वरित खोज, देवता अनुसार भजन सूची और ऑफ़लाइन प्लेबैक के साथ उन्नत भजन-आरती प्लेयर।
  • एज-टू-एज स्क्रीन अनुभव और अधिक सहज पूजा एनिमेशन।
  ```

---

## 4. Google Play Console App Content & Policy Declarations

*(Note: Google Play's API does not allow third-party tools like Fastlane to submit policy questionnaires; complete these once in the **App content** tab of Play Console.)*

1. **Store Settings**:
   - **App or Game**: `App`
   - **Free or Paid**: `Free`
   - **Category**: `Lifestyle`
   - **Tags**: `Religion & spirituality`, `Lifestyle`, `Personalization`, `Music & audio`
   - **Privacy Policy URL**: `https://pavitramandir.app/privacy-policy.html` (source in [`web/privacy-policy.html`](file:///Users/bhuwanjoshi/work/worship/web/privacy-policy.html))
2. **Ads Declaration**:
   - *Does your app contain ads?* → **No, my app does not contain ads**.
3. **App Access**:
   - *All functionality is available without special access* (100% of daily worship, shrines, and bhajans work without login; Google Sign-In is optional for syncing Punya Coins and customized shrine selections).
4. **Content Rating (IARC)**:
   - Category: **Utility, Productivity, Communication, or Other** → Answer **No** to all violence, sexuality, gambling, and unrestricted user-to-user content questions → Rated **Everyone / 3+**.
5. **Target Audience and Content**:
   - Target age groups: **13–15, 16–17, 18 and over** (Do not select under-13 to avoid Families Policy restrictions on Google Sign-In).
6. **Data Safety Questionnaire**:
   - *Does your app collect or share any of the required user data types?* → **Yes** (only when a user optionally signs in with Google or uses the account sync service):
     - **Personal info → Name & Email address**: Collected, Optional (for Account Management / Google Sign-In backup), Not shared with third parties, Encrypted in transit (HTTPS/TLS), Users can request deletion.
     - **App activity → Other actions (Devotion streak, Punya Coin ledger, unlocked shrine options)**: Collected, Optional (for App Functionality & Account sync), Not shared, Encrypted in transit, Users can request deletion.
   - **Account Deletion URL**: `https://pavitramandir.app/privacy-policy.html` (see section 4 & 5 of [`web/privacy-policy.html`](file:///Users/bhuwanjoshi/work/worship/web/privacy-policy.html)).
