package music

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestCatalogAndConditionalGet(t *testing.T) {
	h, err := New("")
	if err != nil {
		t.Fatal(err)
	}
	r := httptest.NewRecorder()
	h.ServeHTTP(r, httptest.NewRequest("GET", "/api/v2/music", nil))
	var c Catalog
	if err := json.Unmarshal(r.Body.Bytes(), &c); err != nil {
		t.Fatal(err)
	}
	if len(c.Tracks) == 0 {
		t.Fatal("empty catalog")
	}
	for _, track := range c.Tracks {
		if track.AudioURL == "" || track.BitrateKbps < 112 || track.SampleRateHz < 32000 || track.DurationSeconds < 60 {
			t.Fatal("unverified recording", track.ID)
		}
		if track.VolumeGainDb > 0 || track.VolumeGainDb < -12 {
			t.Fatal("unsafe amplification", track.ID)
		}
		if track.Order >= 81 && track.Order <= 100 && track.Collection != "mixed" {
			t.Fatal("mixed collection lost", track.ID)
		}
	}
	req := httptest.NewRequest("GET", "/api/v2/music", nil)
	req.Header.Set("If-None-Match", r.Header().Get("ETag"))
	cached := httptest.NewRecorder()
	h.ServeHTTP(cached, req)
	if cached.Code != http.StatusNotModified || cached.Body.Len() != 0 {
		t.Fatal("conditional GET failed")
	}
	denied := httptest.NewRecorder()
	h.ServeHTTP(denied, httptest.NewRequest("POST", "/api/v2/music", nil))
	if denied.Code != http.StatusMethodNotAllowed {
		t.Fatal("mutation permitted")
	}
}
