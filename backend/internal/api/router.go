package api

import (
	"net/http"
)

// CorsMiddleware attaches standard permissive headers for local cross-origin development.
func CorsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-ID, X-Client-Time")

		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

// NewRouter sets up all endpoints using standard library ServeMux.
func NewRouter(handler *Handler) http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("GET /api/v1/mandir/state", handler.GetMandirState)
	mux.HandleFunc("POST /api/v1/mandir/pooja/complete", handler.CompletePooja)
	mux.HandleFunc("POST /api/v1/mandir/clean", handler.CleanMandir)
	mux.HandleFunc("GET /api/v1/panchang/today", handler.GetTodayPanchang)
	mux.HandleFunc("GET /api/v1/aartis", handler.GetAartis)

	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"healthy","service":"nityamandir-backend"}`))
	})

	return CorsMiddleware(mux)
}
