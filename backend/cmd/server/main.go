package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/worship/nityamandir/backend/internal/api"
	"github.com/worship/nityamandir/backend/internal/community"
	"github.com/worship/nityamandir/backend/internal/googleauth"
	"github.com/worship/nityamandir/backend/internal/music"
	"github.com/worship/nityamandir/backend/internal/observability"
	"github.com/worship/nityamandir/backend/internal/otp"
	"github.com/worship/nityamandir/backend/internal/repository"
	"github.com/worship/nityamandir/backend/internal/service"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	// Initialize dependency graph
	repo := repository.NewMemoryRepository()
	mandirService := service.NewMandirService(repo)
	panchangService := service.NewPanchangService()
	aartiService := service.NewAartiService()

	handler := api.NewHandler(mandirService, panchangService, aartiService)
	legacy := api.NewRouter(handler)
	dataPath := os.Getenv("ACCOUNT_DATA_PATH")
	if dataPath == "" {
		dataPath = "data/accounts.json"
	}
	accounts, err := community.New(dataPath)
	if err != nil {
		log.Fatal(err)
	}
	accounts.MockMode = os.Getenv("MOCK_MODE") == "true"
	accounts.OTP, err = otp.FromEnv()
	if err != nil {
		log.Fatal(err)
	}
	if accounts.OTP != nil && accounts.MockMode {
		log.Fatal("Disable MOCK_MODE when configuring live OTP")
	}
	if clientID := os.Getenv("GOOGLE_CLIENT_ID"); clientID != "" {
		accounts.Google, err = googleauth.New(clientID)
		if err != nil {
			log.Fatal(err)
		}
	}
	router := http.NewServeMux()
	musicHandler, err := music.New(os.Getenv("MUSIC_CATALOG_PATH"))
	if err != nil {
		log.Fatal(err)
	}
	router.Handle("/api/v2/music", musicHandler)
	router.Handle("/api/v2/", accounts)
	// Legacy prototype routes are opt-in: they trust arbitrary user headers.
	if os.Getenv("ENABLE_LEGACY_DEMO") == "true" {
		router.Handle("/api/v1/", legacy)
	}
	router.Handle("/healthz", legacy)
	monitor := observability.New()
	bind := os.Getenv("APP_BIND")
	if bind == "" {
		bind = "127.0.0.1"
	}
	metricsAddr := os.Getenv("METRICS_ADDR")
	if metricsAddr == "" {
		metricsAddr = "127.0.0.1:9091"
	}

	server := &http.Server{
		Addr:         bind + ":" + port,
		Handler:      monitor.Wrap(router),
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}
	metricsMux := http.NewServeMux()
	metricsMux.Handle("GET /metrics", monitor.Metrics())
	metricsServer := &http.Server{Addr: metricsAddr, Handler: metricsMux, ReadHeaderTimeout: 5 * time.Second}

	// Graceful shutdown channel
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	go func() {
		log.Printf("ॐ Pavitra Mandir (पवित्र मंदिर) Backend Service running on port %s...", port)
		log.Printf("Account API: /api/v2/ (catalog, register, login, me, activity/puja, shrine)")
		log.Printf("Health: /healthz; legacy demo enabled: %t", os.Getenv("ENABLE_LEGACY_DEMO") == "true")
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server listen failed: %v", err)
		}
	}()
	go func() {
		log.Printf("Private metrics listening on %s", metricsAddr)
		if err := metricsServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Metrics listen failed: %v", err)
		}
	}()

	<-stop
	log.Println("Shutting down Pavitra Mandir server gracefully...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}
	if err := metricsServer.Shutdown(ctx); err != nil {
		log.Fatalf("Metrics server forced to shutdown: %v", err)
	}

	fmt.Println("Server exited cleanly. शुभम् अस्तु।")
}
