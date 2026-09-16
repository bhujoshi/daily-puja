package repository

import (
	"sync"
	"time"

	"github.com/worship/nityamandir/backend/internal/model"
)

// Repository provides thread-safe access to mandir state.
type Repository interface {
	GetMandirState(userID string) (*model.MandirState, error)
	SaveMandirState(state *model.MandirState) error
	RecordPooja(userID string, completedAt time.Time, steps []string) (*model.MandirState, error)
	RecordClean(userID string, cleanedAt time.Time) (*model.MandirState, error)
}

type memoryRepo struct {
	mu     sync.RWMutex
	states map[string]*model.MandirState
}

// NewMemoryRepository initializes an in-memory thread-safe repository.
func NewMemoryRepository() Repository {
	return &memoryRepo{
		states: make(map[string]*model.MandirState),
	}
}

func (r *memoryRepo) GetMandirState(userID string) (*model.MandirState, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	state, exists := r.states[userID]
	if !exists {
		// Create default authentic Mandir setup
		now := time.Now().UTC()
		// Default to yesterday's pooja so new users can experience the morning cleanliness & cleaning or fresh worship
		yesterday := now.Add(-20 * time.Hour)
		state = &model.MandirState{
			UserID:               userID,
			TempleStyle:          "sheesham_wood",
			ActiveDeities:        []string{"ganesh_ji", "lakshmi_ji"},
			CurtainsClosed:       true,
			CleanlinessConfirmed: false,
			LastWorshipTime:      &yesterday,
			LastCleanedTime:      &yesterday,
			DevotionStreak: model.DevotionStreak{
				CurrentStreakDays: 1,
				LongestStreakDays: 1,
				TodayCompleted:    false,
			},
			DailySuvichar: model.DailySuvichar{
				Sanskrit: "सत्यं वद धर्मं चर। स्वाध्यायान्मा प्रमदः।",
				Hindi:    "सत्य बोलो, धर्म का आचरण करो, और स्वाध्याय में प्रमाद मत करो।",
				English:  "Speak the truth, practice righteousness, and do not neglect self-study.",
			},
		}
		r.states[userID] = state
	}
	return state, nil
}

func (r *memoryRepo) SaveMandirState(state *model.MandirState) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.states[state.UserID] = state
	return nil
}

func (r *memoryRepo) RecordPooja(userID string, completedAt time.Time, steps []string) (*model.MandirState, error) {
	state, err := r.GetMandirState(userID)
	if err != nil {
		return nil, err
	}

	r.mu.Lock()
	defer r.mu.Unlock()

	state.LastWorshipTime = &completedAt
	state.LastCleanedTime = &completedAt
	state.CleanlinessConfirmed = true
	state.CurtainsClosed = false
	state.DevotionStreak.CurrentStreakDays++
	if state.DevotionStreak.CurrentStreakDays > state.DevotionStreak.LongestStreakDays {
		state.DevotionStreak.LongestStreakDays = state.DevotionStreak.CurrentStreakDays
	}
	state.DevotionStreak.TodayCompleted = true

	return state, nil
}

func (r *memoryRepo) RecordClean(userID string, cleanedAt time.Time) (*model.MandirState, error) {
	state, err := r.GetMandirState(userID)
	if err != nil {
		return nil, err
	}

	r.mu.Lock()
	defer r.mu.Unlock()

	state.LastCleanedTime = &cleanedAt
	return state, nil
}
