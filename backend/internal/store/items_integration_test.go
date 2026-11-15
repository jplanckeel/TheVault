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

// Covers survive app syncs and empty IGDB answers (TEST_DATABASE_URL).
func TestItemStore_CoverProtection_Postgres(t *testing.T) {
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

	console, err := items.Upsert(ctx, domain.Item{ExternalID: "cover-console", Type: "console", Title: "GameCube", Platform: "GameCube"})
	if err != nil {
		t.Fatal(err)
	}
	if err := items.UpdateIGDB(ctx, console.ID, domain.IGDBData{CoverURL: "https://img/logo.png"}); err != nil {
		t.Fatal(err)
	}
	// The app syncs the console again without a picture.
	again, err := items.Upsert(ctx, domain.Item{ExternalID: "cover-console", Type: "console", Title: "GameCube", Platform: "GameCube"})
	if err != nil || again.CoverURL != "https://img/logo.png" {
		t.Fatalf("cover after sync = %q, %v", again.CoverURL, err)
	}

	game, err := items.Upsert(ctx, domain.Item{ExternalID: "cover-game", Type: "game", Title: "Zelda", Platform: "GameCube"})
	if err != nil {
		t.Fatal(err)
	}
	if err := items.UpdateCoverURL(ctx, game.ID, "/covers/x.webp"); err != nil {
		t.Fatal(err)
	}
	if err := items.UpdateIGDB(ctx, game.ID, domain.IGDBData{IgdbID: "42", CoverURL: "https://img/igdb.jpg", Genres: "Action"}); err != nil {
		t.Fatal(err)
	}
	got, err := items.GetByID(ctx, game.ID)
	if err != nil || got.CoverURL != "/covers/x.webp" || got.Genres != "Action" || !got.AdminEdited {
		t.Fatalf("game = %+v, %v", got, err)
	}

	unknown, err := items.Upsert(ctx, domain.Item{ExternalID: "cover-unknown", Type: "game", Title: "Unknown", Platform: "Wii"})
	if err != nil {
		t.Fatal(err)
	}
	if err := items.MarkIGDBChecked(ctx, unknown.ID); err != nil {
		t.Fatal(err)
	}
	pending, err := items.ListWithoutIGDB(ctx)
	if err != nil {
		t.Fatal(err)
	}
	for _, it := range pending {
		if it.ID == unknown.ID {
			t.Errorf("an item checked today waits a week before the next IGDB attempt")
		}
	}
}
