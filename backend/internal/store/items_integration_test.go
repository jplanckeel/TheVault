package store

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// Runs against a migrated database when TEST_DATABASE_URL is set.
func TestItemStore_LookupAndPricing_Postgres(t *testing.T) {
	url := os.Getenv("TEST_DATABASE_URL")
	if url == "" {
		t.Skip("TEST_DATABASE_URL not set")
	}
	ctx := context.Background()
	pool, err := pgxpool.New(ctx, url)
	if err != nil {
		t.Fatal(err)
	}
	defer pool.Close()
	items := NewItemStore(pool)
	prices := NewPriceStore(pool)

	quoted, err := items.Upsert(ctx, domain.Item{ExternalID: "lookup-quoted", Type: "game", Title: "Quoted", Platform: "GameCube", CoverURL: "https://img/q.jpg"})
	if err != nil {
		t.Fatal(err)
	}
	fresh, err := items.Upsert(ctx, domain.Item{ExternalID: "lookup-fresh", Type: "game", Title: "Fresh", Platform: "Wii"})
	if err != nil {
		t.Fatal(err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO item_aliases (external_id, item_id) VALUES ('lookup-alias', $1)`, quoted.ID); err != nil {
		t.Fatal(err)
	}
	// Insert stamps NOW(): back-date the snapshot by hand.
	if _, err := pool.Exec(ctx, `INSERT INTO price_snapshots (item_id, source, condition, price_median, scraped_at) VALUES ($1, 'ebay_pal', 'cib', 10, NOW() - interval '2 days')`, quoted.ID); err != nil {
		t.Fatal(err)
	}
	recent := domain.PriceSnapshot{ItemID: fresh.ID, Source: "ebay_pal", Condition: "cib", PriceMedian: 10, ScrapedAt: time.Now()}
	if _, err := prices.Insert(ctx, recent); err != nil {
		t.Fatal(err)
	}

	entries, err := items.LookupByExternalIDs(ctx, []string{"lookup-alias", "lookup-fresh", "nope"})
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 2 || entries[0].ExternalID != "lookup-alias" || entries[0].CoverURL != "https://img/q.jpg" {
		t.Fatalf("lookup = %+v", entries)
	}

	stale, err := items.ListForPricing(ctx, 24*time.Hour, 10)
	if err != nil {
		t.Fatal(err)
	}
	for _, it := range stale {
		if it.ID == fresh.ID {
			t.Errorf("an item quoted today is not stale")
		}
	}
	found := false
	for _, it := range stale {
		found = found || it.ID == quoted.ID
	}
	if !found {
		t.Errorf("an item quoted two days ago is stale")
	}
}
