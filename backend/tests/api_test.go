package tests

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/worship/nityamandir/backend/internal/api"
	"github.com/worship/nityamandir/backend/internal/model"
	"github.com/worship/nityamandir/backend/internal/repository"
	"github.com/worship/nityamandir/backend/internal/service"
)

func setupTestServer() http.Handler {
	repo := repository.NewMemoryRepository()
	mandirService := service.NewMandirService(repo)
	panchangService := service.NewPanchangService()
	aartiService := service.NewAartiService()

	handler := api.NewHandler(mandirService, panchangService, aartiService)
	return api.NewRouter(handler)
}

func TestGetMandirState(t *testing.T) {
	router := setupTestServer()

	req, _ := http.NewRequest("GET", "/api/v1/mandir/state", nil)
	req.Header.Set("X-User-ID", "test_bhakt_1")
	w := httptest.NewRecorder()

	router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d. Body: %s", w.Code, w.Body.String())
	}

	var state model.MandirState
	if err := json.Unmarshal(w.Body.Bytes(), &state); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}

	if state.UserID != "test_bhakt_1" {
		t.Errorf("expected user_id test_bhakt_1, got %s", state.UserID)
	}
	if len(state.ActiveDeities) != 2 || state.ActiveDeities[0] != "ganesh_ji" || state.ActiveDeities[1] != "lakshmi_ji" {
		t.Errorf("expected default deities [ganesh_ji, lakshmi_ji], got %v", state.ActiveDeities)
	}
}

func TestPoojaCompletionGuardrail(t *testing.T) {
	router := setupTestServer()

	// 1. Incomplete steps (Guardrail should reject)
	badBody, _ := json.Marshal(model.PoojaSessionRequest{
		SessionID:      "sess_bad",
		CompletedAt:    time.Now().UTC(),
		StepsCompleted: []string{"samagri_sangrah", "deepa_prajwalan"}, // only 2 steps
	})
	badReq, _ := http.NewRequest("POST", "/api/v1/mandir/pooja/complete", bytes.NewReader(badBody))
	badReq.Header.Set("X-User-ID", "test_bhakt_2")
	badW := httptest.NewRecorder()

	router.ServeHTTP(badW, badReq)
	if badW.Code != http.StatusBadRequest {
		t.Fatalf("expected status 400 for incomplete ritual, got %d", badW.Code)
	}

	// 2. Complete 8 steps (Success)
	goodBody, _ := json.Marshal(model.PoojaSessionRequest{
		SessionID:   "sess_good",
		CompletedAt: time.Now().UTC(),
		StepsCompleted: []string{
			"samagri_sangrah",
			"deepa_prajwalan",
			"dev_snan",
			"tilak_pushparpan",
			"ghanti_aarti",
			"shankh_naad",
			"bhog_samarpan",
			"aarti_stuti",
		},
		AartiPlayedID: "jai_ganesh_deva",
	})
	goodReq, _ := http.NewRequest("POST", "/api/v1/mandir/pooja/complete", bytes.NewReader(goodBody))
	goodReq.Header.Set("X-User-ID", "test_bhakt_2")
	goodW := httptest.NewRecorder()

	router.ServeHTTP(goodW, goodReq)
	if goodW.Code != http.StatusOK {
		t.Fatalf("expected status 200 for full ritual, got %d. Body: %s", goodW.Code, goodW.Body.String())
	}

	var resp model.PoojaSessionResponse
	if err := json.Unmarshal(goodW.Body.Bytes(), &resp); err != nil {
		t.Fatalf("failed to decode pooja response: %v", err)
	}
	if resp.Status != "success" {
		t.Errorf("expected success status, got %s", resp.Status)
	}
	if resp.NewStreakDays < 1 {
		t.Errorf("expected streak to be incremented, got %d", resp.NewStreakDays)
	}
}

func TestCleanMandir(t *testing.T) {
	router := setupTestServer()

	cleanBody, _ := json.Marshal(model.CleanMandirRequest{
		CleanedAt:         time.Now().UTC(),
		NirmalyaCollected: true,
		ChoukiWiped:       true,
		DiyaWashed:        true,
	})
	req, _ := http.NewRequest("POST", "/api/v1/mandir/clean", bytes.NewReader(cleanBody))
	req.Header.Set("X-User-ID", "test_bhakt_3")
	w := httptest.NewRecorder()

	router.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", w.Code)
	}

	var resp model.CleanMandirResponse
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("failed to decode: %v", err)
	}
	if resp.DustLevel != 0.0 || resp.NeedsCleaning {
		t.Errorf("expected cleaned mandir with dust 0.0, got dust %v, needsCleaning %v", resp.DustLevel, resp.NeedsCleaning)
	}
}

func TestGetPanchangAndAartis(t *testing.T) {
	router := setupTestServer()

	// Panchang
	pReq, _ := http.NewRequest("GET", "/api/v1/panchang/today", nil)
	pW := httptest.NewRecorder()
	router.ServeHTTP(pW, pReq)
	if pW.Code != http.StatusOK {
		t.Fatalf("expected 200 for panchang, got %d", pW.Code)
	}

	// Aartis
	aReq, _ := http.NewRequest("GET", "/api/v1/aartis", nil)
	aW := httptest.NewRecorder()
	router.ServeHTTP(aW, aReq)
	if aW.Code != http.StatusOK {
		t.Fatalf("expected 200 for aartis, got %d", aW.Code)
	}

	var aartis []model.AartiItem
	if err := json.Unmarshal(aW.Body.Bytes(), &aartis); err != nil {
		t.Fatalf("failed to decode aartis: %v", err)
	}
	if len(aartis) < 3 {
		t.Errorf("expected at least 3 authentic aartis, got %d", len(aartis))
	}
}
