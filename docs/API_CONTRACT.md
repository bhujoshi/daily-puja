# Nitya Mandir (नित्य मंदिर) - API Specification & Contract

The Nitya Mandir backend exposes a lightweight, high-performance RESTful API written in Go. It supports offline-first synchronization, daily devotion streaks, temporal aging reconciliation, and sacred audio catalog delivery.

**Base URL**: `/api/v1`  
**Content-Type**: `application/json; charset=utf-8`

---

## 1. Endpoints Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/mandir/state` | Fetch current mandir state, aging calculation, and daily thought |
| `POST` | `/mandir/pooja/complete` | Record completed 8-step daily pooja, update streak |
| `POST` | `/mandir/clean` | Record morning temple cleaning event, reset dust level |
| `GET` | `/panchang/today` | Fetch daily auspicious timings (Brahma Muhurta, Aarti times) |
| `GET` | `/aartis` | Fetch verified Aartis catalog with synced bilingual lyrics |

---

## 2. Detailed Endpoint Contracts

### 2.1 `GET /mandir/state`
Returns the user's mandir configuration and server-computed temporal aging metrics.

#### Request Headers:
- `X-User-ID`: String (e.g. `user_108`)
- `X-Client-Time`: ISO 8601 Timestamp (e.g. `2026-09-14T06:30:00Z`)

#### Response (`200 OK`):
```json
{
  "user_id": "user_108",
  "temple_style": "sheesham_wood",
  "active_deities": ["ganesh_ji", "lakshmi_ji"],
  "curtains_closed": true,
  "cleanliness_confirmed": false,
  "last_worship_time": "2026-09-13T07:15:00Z",
  "last_cleaned_time": "2026-09-13T07:00:00Z",
  "aging_metrics": {
    "elapsed_hours": 23.25,
    "dust_level": 0.128,
    "flower_wither_factor": 0.468,
    "needs_cleaning": true,
    "status_description": "कल के फूल निर्माल्य हो चुके हैं। मंदिर की सफाई करें।"
  },
  "devotion_streak": {
    "current_streak_days": 14,
    "longest_streak_days": 21,
    "today_completed": false
  },
  "daily_suvichar": {
    "sanskrit": "सत्यं वद धर्मं चर। स्वाध्यायान्मा प्रमदः।",
    "hindi": "सत्य बोलो, धर्म का आचरण करो, और स्वाध्याय में प्रमाद मत करो।",
    "english": "Speak the truth, practice righteousness, and do not neglect self-study."
  }
}
```

---

### 2.2 `POST /mandir/pooja/complete`
Registers the successful, respectful completion of all 8 steps of the daily pooja.

#### Request Body:
```json
{
  "session_id": "sess_89432",
  "completed_at": "2026-09-14T07:10:00Z",
  "steps_completed": [
    "samagri_sangrah",
    "deepa_prajwalan",
    "dev_snan",
    "tilak_pushparpan",
    "ghanti_aarti",
    "shankh_naad",
    "bhog_samarpan",
    "aarti_stuti"
  ],
  "aarti_played_id": "jai_ganesh_deva",
  "client_signature": "sha256_sig_abc123"
}
```

#### Response (`200 OK`):
```json
{
  "status": "success",
  "message": "पूजा संपन्न हुई। आपका दिन मंगलमय हो!",
  "new_streak_days": 15,
  "last_worship_time": "2026-09-14T07:10:00Z",
  "blessing": {
    "title": "विघ्नहर्ता गणेश व महालक्ष्मी आशीर्वाद",
    "message": "May your home be filled with peace, wisdom, and auspicious prosperity."
  }
}
```

---

### 2.3 `POST /mandir/clean`
Invoked when the user completes the morning cleaning ritual (nirmalya collection, wiping the chouki, washing the diya).

#### Request Body:
```json
{
  "cleaned_at": "2026-09-14T06:45:00Z",
  "nirmalya_collected": true,
  "chouki_wiped": true,
  "diya_washed": true
}
```

#### Response (`200 OK`):
```json
{
  "status": "success",
  "message": "मंदिर पूर्णतः स्वच्छ व पवित्र है। अब आप पूजा आरंभ कर सकते हैं।",
  "dust_level": 0.0,
  "flower_wither_factor": 0.0,
  "needs_cleaning": false
}
```

---

### 2.4 `GET /panchang/today`
Returns auspicious Vedic time slots for the user's coordinates/timezone.

#### Response (`200 OK`):
```json
{
  "date": "2026-09-14",
  "tithi": "शुक्ल चतुर्थी (गणेश चतुर्थी मुहूर्त)",
  "brahma_muhurta": {
    "start": "04:32 AM",
    "end": "05:20 AM"
  },
  "pratah_sandhya": {
    "start": "04:56 AM",
    "end": "06:05 AM"
  },
  "sayankal_aarti": {
    "start": "06:45 PM",
    "end": "07:30 PM"
  }
}
```

---

### 2.5 `GET /aartis`
Returns verified Aartis with synchronized bilingual lyric markers for karaoke-style reading.

#### Response (`200 OK`):
```json
[
  {
    "id": "jai_ganesh_deva",
    "title_hi": "जय गणेश देवा",
    "title_en": "Jai Ganesh Deva",
    "deity": "ganesh_ji",
    "duration_seconds": 184,
    "audio_url": "https://assets.nityamandir.com/audio/jai_ganesh_deva.mp3",
    "lyrics": [
      {
        "timestamp_ms": 0,
        "line_hi": "जय गणेश, जय गणेश, जय गणेश देवा।",
        "line_en": "Jai Ganesh, Jai Ganesh, Jai Ganesh Deva.",
        "meaning_en": "Glory to You, O Lord Ganesha, the divine supreme."
      },
      {
        "timestamp_ms": 6200,
        "line_hi": "माता जाकी पार्वती, पिता महादेवा॥",
        "line_en": "Mata jaaki Parvati, Pita Mahadeva.",
        "meaning_en": "Whose mother is Divine Parvati, and father is Mahadeva Shiva."
      }
    ]
  }
]
```
