package service

import (
	"github.com/worship/nityamandir/backend/internal/model"
)

// AartiService provides verified sacred aartis and synchronized lyrics.
type AartiService struct{}

// NewAartiService initializes the Aarti service.
func NewAartiService() *AartiService {
	return &AartiService{}
}

// GetAllAartis returns the authentic hymn catalog.
func (s *AartiService) GetAllAartis() []model.AartiItem {
	return []model.AartiItem{
		{
			ID:              "jai_ganesh_deva",
			TitleHi:         "जय गणेश देवा",
			TitleEn:         "Jai Ganesh Deva",
			Deity:           "ganesh_ji",
			DurationSeconds: 195,
			AudioURL:        "/audio/jai_ganesh_deva.mp3",
			Lyrics: []model.AartiLyricLine{
				{
					TimestampMs: 0,
					LineHi:      "जय गणेश, जय गणेश, जय गणेश देवा।",
					LineEn:      "Jai Ganesh, Jai Ganesh, Jai Ganesh Deva.",
					MeaningEn:   "Glory to You, O Lord Ganesha, the divine supreme.",
				},
				{
					TimestampMs: 6500,
					LineHi:      "माता जाकी पार्वती, पिता महादेवा॥",
					LineEn:      "Mata jaaki Parvati, Pita Mahadeva.",
					MeaningEn:   "Whose mother is Divine Parvati, and father is Mahadeva Shiva.",
				},
				{
					TimestampMs: 13000,
					LineHi:      "एक दन्त दयावन्त, चार भुजाधारी।",
					LineEn:      "Ek danta dayavanta, chaar bhujaadhaari.",
					MeaningEn:   "The one-tusked, merciful Lord, who possesses four arms.",
				},
				{
					TimestampMs: 19500,
					LineHi:      "माथे पर तिलक सोहे, मूसे की सवारी॥",
					LineEn:      "Maathe par tilak sohe, moose ki savaari.",
					MeaningEn:   "Whose forehead is adorned with tilak, who rides upon the mouse.",
				},
				{
					TimestampMs: 26000,
					LineHi:      "पान चढ़े, फूल चढ़े, और चढ़े मेवा।",
					LineEn:      "Paan chadhe, phool chadhe, aur chadhe meva.",
					MeaningEn:   "Betel leaves, fragrant blossoms, and dry fruits are offered.",
				},
				{
					TimestampMs: 32500,
					LineHi:      "लड्डुअन का भोग लगे, सन्त करें सेवा॥",
					LineEn:      "Ladduan ka bhog lage, sant karein seva.",
					MeaningEn:   "Sweet laddoos are offered as bhog, and devotees serve with love.",
				},
			},
		},
		{
			ID:              "om_jai_lakshmi_mata",
			TitleHi:         "ॐ जय लक्ष्मी माता",
			TitleEn:         "Om Jai Lakshmi Mata",
			Deity:           "lakshmi_ji",
			DurationSeconds: 210,
			AudioURL:        "/audio/om_jai_lakshmi_mata.mp3",
			Lyrics: []model.AartiLyricLine{
				{
					TimestampMs: 0,
					LineHi:      "ॐ जय लक्ष्मी माता, मैया जय लक्ष्मी माता।",
					LineEn:      "Om Jai Lakshmi Mata, Maiya Jai Lakshmi Mata.",
					MeaningEn:   "Glory to You, O Mother Lakshmi, Goddess of Grace and Auspiciousness.",
				},
				{
					TimestampMs: 7000,
					LineHi:      "तुमको निसदिन सेवत, हर विष्णु विधाता॥",
					LineEn:      "Tumko nisdin sevat, Hari Vishnu Vidhaata.",
					MeaningEn:   "Lord Shiva, Lord Vishnu, and Brahma meditate upon You every day.",
				},
				{
					TimestampMs: 14000,
					LineHi:      "उमा, रमा, ब्रह्माणी, तुम ही जग-माता।",
					LineEn:      "Uma, Rama, Brahmaani, tum hi Jag-Mata.",
					MeaningEn:   "You manifest as Parvati, Lakshmi, and Saraswati, Mother of the Universe.",
				},
				{
					TimestampMs: 21000,
					LineHi:      "सूर्य-चन्द्रमा ध्यावत, नारद ऋषि गाता॥",
					LineEn:      "Surya-Chandrama dhyaavat, Narad Rishi gaata.",
					MeaningEn:   "The Sun and Moon revere You, and Sage Narada sings Your praise.",
				},
			},
		},
		{
			ID:              "sukhkarta_dukhharta",
			TitleHi:         "सुखकर्ता दुखहर्ता",
			TitleEn:         "Sukhkarta Dukhharta",
			Deity:           "ganesh_ji",
			DurationSeconds: 180,
			AudioURL:        "/audio/sukhkarta_dukhharta.mp3",
			Lyrics: []model.AartiLyricLine{
				{
					TimestampMs: 0,
					LineHi:      "सुखकर्ता दुखहर्ता वार्ता विघ्नाची।",
					LineEn:      "Sukhkarta dukhharta vaarta vighnaachi.",
					MeaningEn:   "Bestower of joy, dispeller of sorrow, destroyer of all obstacles.",
				},
				{
					TimestampMs: 6000,
					LineHi:      "नुरवी पुरवी प्रेम कृपा जयाची॥",
					LineEn:      "Nurvee purvee prem kripa jayaachi.",
					MeaningEn:   "Who showers endless love and grace upon devotees.",
				},
			},
		},
	}
}
