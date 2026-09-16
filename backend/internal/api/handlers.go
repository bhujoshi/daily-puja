package api

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/worship/nityamandir/backend/internal/model"
	"github.com/worship/nityamandir/backend/internal/service"
)

// Handler handles all incoming HTTP requests.
type Handler struct {
	mandirService  *service.MandirService
	panchangService *service.PanchangService
	aartiService   *service.AartiService
}

// NewHandler initializes the API handler.
func NewHandler(
	mandirService *service.MandirService,
	panchangService *service.PanchangService,
	aartiService *service.AartiService,
) *Handler {
	return &Handler{
		mandirService:   mandirService,
		panchangService: panchangService,
		aartiService:    aartiService,
	}
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, msg string) {
	writeJSON(w, status, map[string]string{"error": msg})
}

// GetMandirState handles GET /api/v1/mandir/state.
func (h *Handler) GetMandirState(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("X-User-ID")
	if userID == "" {
		userID = "default_bhakt"
	}

	clientTime := time.Now().UTC()
	if ctStr := r.Header.Get("X-Client-Time"); ctStr != "" {
		if parsed, err := time.Parse(time.RFC3339, ctStr); err == nil {
			clientTime = parsed
		}
	}

	state, err := h.mandirService.GetState(userID, clientTime)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, state)
}

// CompletePooja handles POST /api/v1/mandir/pooja/complete.
func (h *Handler) CompletePooja(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("X-User-ID")
	if userID == "" {
		userID = "default_bhakt"
	}

	var req model.PoojaSessionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if req.CompletedAt.IsZero() {
		req.CompletedAt = time.Now().UTC()
	}

	resp, err := h.mandirService.CompletePooja(userID, req)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}

// CleanMandir handles POST /api/v1/mandir/clean.
func (h *Handler) CleanMandir(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("X-User-ID")
	if userID == "" {
		userID = "default_bhakt"
	}

	var req model.CleanMandirRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if req.CleanedAt.IsZero() {
		req.CleanedAt = time.Now().UTC()
	}

	resp, err := h.mandirService.CleanMandir(userID, req)
	if err != nil {
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}

	writeJSON(w, http.StatusOK, resp)
}

// GetTodayPanchang handles GET /api/v1/panchang/today.
func (h *Handler) GetTodayPanchang(w http.ResponseWriter, r *http.Request) {
	data := h.panchangService.GetTodayPanchang(time.Now())
	writeJSON(w, http.StatusOK, data)
}

// GetAartis handles GET /api/v1/aartis.
func (h *Handler) GetAartis(w http.ResponseWriter, r *http.Request) {
	aartis := h.aartiService.GetAllAartis()
	writeJSON(w, http.StatusOK, aartis)
}
