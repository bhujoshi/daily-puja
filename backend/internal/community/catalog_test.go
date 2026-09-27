package community

import "testing"

func TestFlowerSelections(t *testing.T) {
	for _, tc := range []struct {
		flowers string
		valid   bool
	}{
		{"original", true}, {"original,orchid,rose,azalea,bouquet", true},
		{"rose,bouquet", true}, {"", false}, {"rose,rose", false},
		{"rose,missing", false}, {"rose,", false},
	} {
		selections := map[string]string{"shrine": "original", "idols": "original", "flowers": tc.flowers, "lamp": "original", "shankh": "original", "aarti": "original", "prasad": "original"}
		if got := validSelections(selections); got != tc.valid {
			t.Errorf("flowers %q: got %v", tc.flowers, got)
		}
		selections["lamp"] = "original,brass"
		if validSelections(selections) {
			t.Fatal("non-flower categories must stay single-select")
		}
	}
}
