package service

import (
	"time"

	"github.com/worship/nityamandir/backend/internal/model"
)

// PanchangService delivers daily auspicious timings.
type PanchangService struct{}

// NewPanchangService initializes the Panchang service.
func NewPanchangService() *PanchangService {
	return &PanchangService{}
}

// GetTodayPanchang returns auspicious times for the given date.
func (s *PanchangService) GetTodayPanchang(t time.Time) model.PanchangData {
	return model.PanchangData{
		Date:  t.Format("2006-01-02"),
		Tithi: "शुक्ल पक्ष, शुभ मुहूर्त (Auspicious Muhurta)",
		BrahmaMuhurta: model.PanchangTimeSlot{
			Start: "04:30 AM",
			End:   "05:18 AM",
		},
		PratahSandhya: model.PanchangTimeSlot{
			Start: "05:00 AM",
			End:   "06:15 AM",
		},
		SayankalAarti: model.PanchangTimeSlot{
			Start: "06:30 PM",
			End:   "07:30 PM",
		},
	}
}
