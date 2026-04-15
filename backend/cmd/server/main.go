package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/jplanckeel/thevaultbo/internal/api"
	"github.com/jplanckeel/thevaultbo/internal/igdb"
	"github.com/jplanckeel/thevaultbo/internal/scheduler"
	"github.com/jplanckeel/thevaultbo/internal/scraper"
	"github.com/jplanckeel/thevaultbo/internal/store"
	"github.com/jplanckeel/thevaultbo/migrations"
)

func main() {
	databaseURL := getenv("DATABASE_URL", "postgres://postgres:postgres@localhost:5432/thevaultbo?sslmode=disable")
	port := getenv("PORT", "8080")

	ctx := context.Background()

	// Run migrations
	if err := store.RunMigrations(migrations.FS, databaseURL); err != nil {
		log.Fatalf("migrations: %v", err)
	}

	// Open DB pool
	pool, err := store.OpenDB(ctx, databaseURL)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer pool.Close()

	// Build stores
	items := store.NewItemStore(pool)
	prices := store.NewPriceStore(pool)
	jobs := store.NewJobStore(pool)
	settings := store.NewSettingsStore(pool)

	// Build runner
	ebayClientID := getenv("EBAY_CLIENT_ID", "")
	ebayClientSecret := getenv("EBAY_CLIENT_SECRET", "")
	ebayBaseURL := getenv("EBAY_BASE_URL", "")
	opts := []scraper.Option{scraper.WithCredentials(ebayClientID, ebayClientSecret)}
	if ebayBaseURL != "" {
		opts = append(opts, scraper.WithBaseURL(ebayBaseURL))
	}
	runner := scraper.NewEbayRunner(items, prices, jobs, settings, opts...)

	// Start scheduler
	sched := scheduler.New(jobs, runner)
	if err := sched.Start(); err != nil {
		log.Fatalf("scheduler: %v", err)
	}
	defer sched.Stop()

	// Build IGDB client
	igdbClientID := getenv("IGDB_CLIENT_ID", "")
	igdbClientSecret := getenv("IGDB_CLIENT_SECRET", "")
	igdbClient := igdb.New(igdbClientID, igdbClientSecret)

	// Build router
	router := api.NewRouter(items, prices, jobs, settings, runner, igdbClient)

	addr := fmt.Sprintf(":%s", port)
	log.Printf("listening on %s", addr)
	if err := http.ListenAndServe(addr, router); err != nil {
		log.Fatalf("server: %v", err)
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
