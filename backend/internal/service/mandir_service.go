package service

import (
	"errors"
	"time"

	"github.com/worship/nityamandir/backend/internal/model"
	"github.com/worship/nityamandir/backend/internal/repository"
)

// MandirService handles domain logic for the virtual temple.
type MandirService struct {
	repo repository.Repository
}

// NewMandirService creates a new MandirService instance.
func NewMandirService(repo repository.Repository) *MandirService {
	return &MandirService{repo: repo}
}

// GetState fetches current temple state and computes dynamic aging.
func (s *MandirService) GetState(userID string, clientTime time.Time) (*model.MandirState, error) {
	state, err := s.repo.GetMandirState(userID)
	if err != nil {
		return nil, err
	}

	// Compute live aging metrics
	state.AgingMetrics = CalculateAging(state.LastWorshipTime, state.LastCleanedTime, clientTime)
	return state, nil
}

// CompletePooja records the completion of 8-step worship.
func (s *MandirService) CompletePooja(userID string, req model.PoojaSessionRequest) (*model.PoojaSessionResponse, error) {
	// Guardrail: Verify all 8 steps were completed
	expectedSteps := 8
	if len(req.StepsCompleted) < expectedSteps {
		return nil, errors.New("guardrail violation: all 8 sacred steps must be completed in order")
	}

	state, err := s.repo.RecordPooja(userID, req.CompletedAt, req.StepsCompleted)
	if err != nil {
		return nil, err
	}

	resp := &model.PoojaSessionResponse{
		Status:          "success",
		Message:         "पूजा संपन्न हुई। आपका दिन मंगलमय हो!",
		NewStreakDays:   state.DevotionStreak.CurrentStreakDays,
		LastWorshipTime: req.CompletedAt,
	}
	resp.Blessing.Title = "विघ्नहर्ता गणेश व महालक्ष्मी आशीर्वाद"
	resp.Blessing.Message = "May Lord Ganesha and Mother Lakshmi bless your home with peace, wisdom, and auspicious prosperity."

	return resp, nil
}

// CleanMandir records temple cleaning and updates dust levels.
func (s *MandirService) CleanMandir(userID string, req model.CleanMandirRequest) (*model.CleanMandirResponse, error) {
	_, err := s.repo.RecordClean(userID, req.CleanedAt)
	if err != nil {
		return nil, err
	}

	return &model.CleanMandirResponse{
		Status:             "success",
		Message:            "मंदिर पूर्णतः स्वच्छ व पवित्र है। अब आप पूजा आरंभ कर सकते हैं।",
		DustLevel:          0.0,
		FlowerWitherFactor: 0.0,
		NeedsCleaning:      false,
	}, nil
}
