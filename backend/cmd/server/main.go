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
	router := api.NewRouter(handler)

	server := &http.Server{
		Addr:         ":" + port,
		Handler:      router,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Graceful shutdown channel
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	go func() {
		log.Printf("ॐ Pavitra Mandir (पवित्र मंदिर) Backend Service running on port %s...", port)
		log.Printf("Endpoints:")
		log.Printf("  GET  /api/v1/mandir/state")
		log.Printf("  POST /api/v1/mandir/pooja/complete")
		log.Printf("  POST /api/v1/mandir/clean")
		log.Printf("  GET  /api/v1/panchang/today")
		log.Printf("  GET  /api/v1/aartis")
		log.Printf("  GET  /healthz")
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Server listen failed: %v", err)
		}
	}()

	<-stop
	log.Println("Shutting down Pavitra Mandir server gracefully...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	fmt.Println("Server exited cleanly. शुभम् अस्तु।")
}
