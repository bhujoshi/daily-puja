package service

import (
	"math"
	"time"

	"github.com/worship/nityamandir/backend/internal/model"
)

// CalculateAging computes the mathematical dust accumulation and flower withering factors.
func CalculateAging(lastWorshipTime, lastCleanedTime *time.Time, currentTime time.Time) model.AgingMetrics {
	if lastWorshipTime == nil {
		// New temple setup: pristine, ready for first worship
		return model.AgingMetrics{
			ElapsedHours:       0.0,
			DustLevel:          0.0,
			FlowerWitherFactor: 0.0,
			NeedsCleaning:      false,
			StatusDescription:  "मंदिर नवनिर्मित एवं पवित्र है। प्रथम पूजा आरंभ करें।",
		}
	}

	elapsed := currentTime.Sub(*lastWorshipTime)
	if elapsed < 0 {
		elapsed = 0
	}
	elapsedHours := elapsed.Hours()

	// Calculate Dust Level
	var dustLevel float64
	// If the mandir was cleaned more recently than last worship, use that for dust
	cleanElapsedHours := elapsedHours
	if lastCleanedTime != nil && lastCleanedTime.After(*lastWorshipTime) {
		cleanElapsed := currentTime.Sub(*lastCleanedTime)
		if cleanElapsed < 0 {
			cleanElapsed = 0
		}
		cleanElapsedHours = cleanElapsed.Hours()
	}

	if cleanElapsedHours > 14.0 {
		dustLevel = math.Min(1.0, (cleanElapsedHours-14.0)/72.0)
	}

	// Calculate Flower Withering Factor
	var flowerWither float64
	if elapsedHours > 12.0 {
		flowerWither = math.Min(1.0, (elapsedHours-12.0)/24.0)
	}

	// Needs cleaning flag
	needsCleaning := elapsedHours >= 14.0 || dustLevel > 0.05 || flowerWither > 0.1

	// Generate descriptive status
	var status string
	switch {
	case elapsedHours < 12.0:
		status = "मंदिर प्रकाशमान एवं पावन है। (Temple is glowing and sacred.)"
	case elapsedHours < 24.0:
		status = "कल के फूल निर्माल्य हो रहे हैं। पूजा से पूर्व सफाई करें। (Yesterday's flowers have withered. Morning cleaning required.)"
	case elapsedHours < 72.0:
		status = "मंदिर में धूल व निर्माल्य एकत्र हो गया है। कृपा कर मंदिर स्वच्छ करें। (Dust and nirmalya accumulated. Please clean the temple.)"
	default:
		status = "मंदिर अत्यधिक जीर्ण व धूल-धूसरित हो गया है। संपूर्ण शुद्धि आवश्यक है। (Temple is heavily neglected and dusty. Deep cleaning required.)"
	}

	return model.AgingMetrics{
		ElapsedHours:       math.Round(elapsedHours*100) / 100,
		DustLevel:          math.Round(dustLevel*1000) / 1000,
		FlowerWitherFactor: math.Round(flowerWither*1000) / 1000,
		NeedsCleaning:      needsCleaning,
		StatusDescription:  status,
	}
}
