package tests

import (
	"testing"
	"time"

	"github.com/worship/nityamandir/backend/internal/service"
)

func TestCalculateAging(t *testing.T) {
	now := time.Now().UTC()

	tests := []struct {
		name              string
		worshipOffset     time.Duration
		cleanOffset       time.Duration
		expectNeedsClean  bool
		minDust           float64
		maxDust           float64
		minWither         float64
		maxWither         float64
	}{
		{
			name:             "Pristine (2 hours after worship)",
			worshipOffset:    -2 * time.Hour,
			cleanOffset:      -2 * time.Hour,
			expectNeedsClean: false,
			minDust:          0.0,
			maxDust:          0.0,
			minWither:        0.0,
			maxWither:        0.0,
		},
		{
			name:             "Next Morning (18 hours after worship)",
			worshipOffset:    -18 * time.Hour,
			cleanOffset:      -18 * time.Hour,
			expectNeedsClean: true,
			minDust:          0.04, // (18-14)/72 = 0.055
			maxDust:          0.07,
			minWither:        0.2, // (18-12)/24 = 0.25
			maxWither:        0.3,
		},
		{
			name:             "Neglected (48 hours after worship)",
			worshipOffset:    -48 * time.Hour,
			cleanOffset:      -48 * time.Hour,
			expectNeedsClean: true,
			minDust:          0.40, // (48-14)/72 = 0.472
			maxDust:          0.50,
			minWither:        1.0, // (48-12)/24 > 1.0 => 1.0
			maxWither:        1.0,
		},
		{
			name:             "Heavily Neglected (100 hours after worship)",
			worshipOffset:    -100 * time.Hour,
			cleanOffset:      -100 * time.Hour,
			expectNeedsClean: true,
			minDust:          1.0, // (100-14)/72 > 1.0 => 1.0
			maxDust:          1.0,
			minWither:        1.0,
			maxWither:        1.0,
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			worshipTime := now.Add(tc.worshipOffset)
			cleanTime := now.Add(tc.cleanOffset)

			metrics := service.CalculateAging(&worshipTime, &cleanTime, now)

			if metrics.NeedsCleaning != tc.expectNeedsClean {
				t.Errorf("expected NeedsCleaning=%v, got %v", tc.expectNeedsClean, metrics.NeedsCleaning)
			}
			if metrics.DustLevel < tc.minDust || metrics.DustLevel > tc.maxDust {
				t.Errorf("dust level %v outside range [%v, %v]", metrics.DustLevel, tc.minDust, tc.maxDust)
			}
			if metrics.FlowerWitherFactor < tc.minWither || metrics.FlowerWitherFactor > tc.maxWither {
				t.Errorf("flower wither %v outside range [%v, %v]", metrics.FlowerWitherFactor, tc.minWither, tc.maxWither)
			}
		})
	}
}
