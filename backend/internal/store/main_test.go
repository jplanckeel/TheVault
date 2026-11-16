package store

import (
	"fmt"
	"os"
	"testing"

	"github.com/jplanckeel/thevaultbo/migrations"
)

// TestMain migrates the test database when TEST_DATABASE_URL is set, so the
// Postgres integration tests run against the current schema (locally and in CI).
func TestMain(m *testing.M) {
	if url := os.Getenv("TEST_DATABASE_URL"); url != "" {
		if err := RunMigrations(migrations.FS, url); err != nil {
			fmt.Fprintln(os.Stderr, "test migrations:", err)
			os.Exit(1)
		}
	}
	os.Exit(m.Run())
}
