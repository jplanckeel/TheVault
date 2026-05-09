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

	// Build eBay runner
	ebayClientID := getenv("EBAY_CLIENT_ID", "")
	ebayClientSecret := getenv("EBAY_CLIENT_SECRET", "")
	ebayBaseURL := getenv("EBAY_BASE_URL", "")
	opts := []scraper.Option{scraper.WithCredentials(ebayClientID, ebayClientSecret)}
	if ebayBaseURL != "" {
		opts = append(opts, scraper.WithBaseURL(ebayBaseURL))
	}
	ebayRunner := scraper.NewEbayRunner(items, prices, jobs, settings, opts...)

	// Build IGDB client and runner
	igdbClientID := getenv("IGDB_CLIENT_ID", "")
	igdbClientSecret := getenv("IGDB_CLIENT_SECRET", "")
	igdbClient := igdb.New(igdbClientID, igdbClientSecret)
	igdbRunner := igdb.NewRunner(items, jobs, igdbClient)

	// Start scheduler
	ebayCron := getenv("EBAY_CRON", "0 * * * *")  // default: every hour
	igdbCron := getenv("IGDB_CRON", "0 3 * * *")  // default: daily at 03:00
	schedOpts := []scheduler.Option{
		scheduler.WithEbaySchedule(ebayCron),
		scheduler.WithIGDB(igdbRunner, igdbCron),
	}
	sched := scheduler.New(jobs, ebayRunner, schedOpts...)
	if err := sched.Start(); err != nil {
		log.Fatalf("scheduler: %v", err)
	}
	defer sched.Stop()

	// Build router
	staticDir := getenv("STATIC_DIR", "")
	router := api.NewRouter(items, prices, jobs, settings, ebayRunner, igdbRunner, staticDir)

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
