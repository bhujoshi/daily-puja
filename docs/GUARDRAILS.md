# Nitya Mandir (नित्य मंदिर) - Guardrails & Integrity Framework

This document defines the non-negotiable **Cultural Sanctity**, **Ritual Integrity**, **Technical Security**, and **Privacy Guardrails** governing the Nitya Mandir application across all platforms (Backend, Android, and iOS).

---

## 1. Cultural & Religious Sanctity Guardrails (धार्मिक मर्यादा एवं शुचिता)

### 1.1 Pratima Lakshana (Iconographic Authenticity)
- **Deity Integrity**: Murtis (Shri Ganesh Ji, Maa Lakshmi Ji, etc.) must adhere strictly to canonical Puranic and Shilpa Shastra iconography (*प्रतिमा लक्षण*).
- **Prohibited Modifications**:
  - No caricatures, distorted avatars, comical memes, or irreverent modifications.
  - No user-uploaded arbitrary images as deities without administrative iconographic validation.
  - Only recognized traditional emblems (Swastika, Om, Trishul, Shankh, Chakra, Lotus, Gada) are allowed in temple decor.

### 1.2 Canonical Ritual Progression (विधिपूर्वक अनुक्रम)
- Worship rituals follow the timeless Shodashopachara / Ashtapadi sequence:
  $$\text{Shuddhi (Purity Gate)} \longrightarrow \text{Deepa Prajwalan} \longrightarrow \text{Snan} \longrightarrow \text{Tilak \& Pushpa} \longrightarrow \text{Ghanti \& Aarti} \longrightarrow \text{Shankh Naad} \longrightarrow \text{Naivedya} \longrightarrow \text{Aarti Stuti}$$
- **Prohibited Actions**:
  - Steps cannot be inverted (e.g. performing Bhog before Snan or Aarti before Deepa Prajwalan).
  - Worship cannot proceed if the temple is in a neglected/dusty state; the morning cleaning ritual is mandatory on subsequent days.

### 1.3 Sacred Nirmalya Disposal (निर्माल्य विसर्जन मर्यादा)
- **Zero-Disrespect Policy**: Spent flowers, dried garlands, and offering residues are *Nirmalya* (निर्माल्य) and possess lingering divine sanctity.
- **UI Treatment**:
  - They are NEVER discarded into a trash can, dustbin icon, or "delete" action.
  - They are collected into a consecrated brass **Nirmalya Patra (विसर्जन पात्र)** with the visual and symbolic intention of returning them to holy rivers (भू-विसर्जन / गंगा-समर्पण).

### 1.4 Purity Mindfulness Gate (शुद्धि संकल्प)
- Before the sanctum curtains (पट) can part, users must actively affirm their readiness ("हाँ, मैंने स्नान व शुद्धि कर ली है").
- If the user indicates they have not bathed, the app offers the sacred *Snan Mantra* and guidance, gently encouraging traditional preparation without harsh penalization.

### 1.5 Reverent Audio Handling
- Mantras, Vedic Stutis, and Aartis must be sung with correct Sanskrit/Hindi pronunciation and respectful intonation.
- No auto-tuning, techno beats, or irreverent audio overlays.
- Audio automatically fades out smoothly if an incoming phone call occurs or the app is moved to the background, avoiding abrupt stops.

---

## 2. Technical & Architectural Guardrails

### 2.1 100% Offline-First Worship
- Daily worship is an intimate daily duty (*नित्य कर्म*). It must NEVER be halted by lack of internet connectivity, airplane travel, or remote locations.
- **Rule**: All 8 ritual steps, temple physics, sound synthesis, aging calculations, and cleaning mechanics operate completely offline on SQLite / Room / SwiftData.
- Network sync with the Go backend happens opportunistically in the background without blocking the UI.

### 2.2 Anti-Clock-Tampering & Devotion Streak Verification
- **Challenge**: Users may adjust system clocks on their phones to avoid temple dust or fraudulently claim a 365-day devotion streak.
- **Enforcement**:
  1. **Monotonic Clocks**: Clients record session intervals using monotonic continuous uptime clocks (`SystemClock.elapsedRealtime()` on Android, `mach_continuous_time()` on iOS).
  2. **Sequence Signatures**: Pooja completion events store cryptographically signed sequential counters.
  3. **Backend Timestamp Validation**: When syncing with the Go backend, the server reconciles local timestamps with server NTP time, flagging retroactive time jumps.

### 2.3 Thermal & Battery Throttling
- The mandir rendering engine employs dynamic particle effects (candle flame flicker, billowing smoke, water flow).
- **Rule**:
  - Shaders and particle simulations are clamped to 60 FPS under normal conditions.
  - If the OS enters Low Power Mode or battery is below 20%, particle counts are reduced by 50% and throttled to 30 FPS.
  - When the app is backgrounded, all canvas updates and audio loops pause immediately.

### 2.4 Devotional Privacy (गोपनीयता)
- Devotion is strictly between the worshipper and the Divine.
- **Rule**:
  - Zero behavioral advertising trackers or third-party ad SDKs.
  - Pooja sessions, sankalps, and frequency logs are private to the user account and encrypted in transit (TLS 1.3) and at rest (AES-256).
