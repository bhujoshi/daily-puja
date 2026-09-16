package model

import "time"

// AgingMetrics captures the visual degradation and cleaning needs of the mandir.
type AgingMetrics struct {
	ElapsedHours       float64 `json:"elapsed_hours"`
	DustLevel          float64 `json:"dust_level"`           // 0.0 (clean) to 1.0 (thick dust)
	FlowerWitherFactor float64 `json:"flower_wither_factor"` // 0.0 (fresh) to 1.0 (dried nirmalya)
	NeedsCleaning      bool    `json:"needs_cleaning"`
	StatusDescription  string  `json:"status_description"`
}

// DevotionStreak tracks daily worship continuity.
type DevotionStreak struct {
	CurrentStreakDays int  `json:"current_streak_days"`
	LongestStreakDays int  `json:"longest_streak_days"`
	TodayCompleted    bool `json:"today_completed"`
}

// DailySuvichar provides an inspiring daily Vedic shloka.
type DailySuvichar struct {
	Sanskrit string `json:"sanskrit"`
	Hindi    string `json:"hindi"`
	English  string `json:"english"`
}

// MandirState represents the full state of a user's digital temple.
type MandirState struct {
	UserID               string         `json:"user_id"`
	TempleStyle          string         `json:"temple_style"`
	ActiveDeities        []string       `json:"active_deities"`
	CurtainsClosed       bool           `json:"curtains_closed"`
	CleanlinessConfirmed bool           `json:"cleanliness_confirmed"`
	LastWorshipTime      *time.Time     `json:"last_worship_time"`
	LastCleanedTime      *time.Time     `json:"last_cleaned_time"`
	AgingMetrics         AgingMetrics   `json:"aging_metrics"`
	DevotionStreak       DevotionStreak `json:"devotion_streak"`
	DailySuvichar        DailySuvichar  `json:"daily_suvichar"`
}

// PoojaSessionRequest payload when completing an 8-step pooja.
type PoojaSessionRequest struct {
	SessionID       string    `json:"session_id"`
	CompletedAt     time.Time `json:"completed_at"`
	StepsCompleted  []string  `json:"steps_completed"`
	AartiPlayedID   string    `json:"aarti_played_id"`
	ClientSignature string    `json:"client_signature"`
}

// PoojaSessionResponse returned upon successful pooja recording.
type PoojaSessionResponse struct {
	Status          string    `json:"status"`
	Message         string    `json:"message"`
	NewStreakDays   int       `json:"new_streak_days"`
	LastWorshipTime time.Time `json:"last_worship_time"`
	Blessing        struct {
		Title   string `json:"title"`
		Message string `json:"message"`
	} `json:"blessing"`
}

// CleanMandirRequest payload when user finishes cleaning the temple.
type CleanMandirRequest struct {
	CleanedAt         time.Time `json:"cleaned_at"`
	NirmalyaCollected bool      `json:"nirmalya_collected"`
	ChoukiWiped       bool      `json:"chouki_wiped"`
	DiyaWashed        bool      `json:"diya_washed"`
}

// CleanMandirResponse returned after temple cleaning.
type CleanMandirResponse struct {
	Status             string  `json:"status"`
	Message            string  `json:"message"`
	DustLevel          float64 `json:"dust_level"`
	FlowerWitherFactor float64 `json:"flower_wither_factor"`
	NeedsCleaning      bool    `json:"needs_cleaning"`
}

// PanchangTimeSlot represents a holy window in the day.
type PanchangTimeSlot struct {
	Start string `json:"start"`
	End   string `json:"end"`
}

// PanchangData holds daily astrological and worship timings.
type PanchangData struct {
	Date           string           `json:"date"`
	Tithi          string           `json:"tithi"`
	BrahmaMuhurta  PanchangTimeSlot `json:"brahma_muhurta"`
	PratahSandhya  PanchangTimeSlot `json:"pratah_sandhya"`
	SayankalAarti  PanchangTimeSlot `json:"sayankal_aarti"`
}

// AartiLyricLine defines a single timestamped karaoke line.
type AartiLyricLine struct {
	TimestampMs int64  `json:"timestamp_ms"`
	LineHi      string `json:"line_hi"`
	LineEn      string `json:"line_en"`
	MeaningEn   string `json:"meaning_en"`
}

// AartiItem represents a sacred hymn in the catalog.
type AartiItem struct {
	ID              string           `json:"id"`
	TitleHi         string           `json:"title_hi"`
	TitleEn         string           `json:"title_en"`
	Deity           string           `json:"deity"`
	DurationSeconds int              `json:"duration_seconds"`
	AudioURL        string           `json:"audio_url"`
	Lyrics          []AartiLyricLine `json:"lyrics"`
}
