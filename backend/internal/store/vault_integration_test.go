package store

import (
	"context"
	"os"
	"strings"
	"testing"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// Runs against a migrated database when TEST_DATABASE_URL is set.
func TestVaultStore_Postgres(t *testing.T) {
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
	var userID string
	if err := pool.QueryRow(ctx, `INSERT INTO users (email, password_hash, name) VALUES ('vault@test.local', 'x', 'T') RETURNING id`).Scan(&userID); err != nil {
		t.Fatal(err)
	}
	s := NewVaultStore(pool)
	if _, err := s.SaveBackup(ctx, userID, []byte(`{"items":[{"title":"A"}]}`)); err != nil {
		t.Fatal(err)
	}
	if _, err := s.SaveBackup(ctx, userID, []byte(`{"items":[{"title":"B"}]}`)); err != nil {
		t.Fatal(err)
	}
	payload, _, err := s.GetBackup(ctx, userID)
	if err != nil || !strings.Contains(string(payload), "B") {
		t.Fatalf("backup = %s, %v", payload, err)
	}
	sh, err := s.CreateShare(ctx, domain.Share{Token: "tok_0123456789abcdefghij", UserID: userID, Scope: domain.ShareCollection})
	if err != nil || sh.CreatedAt.IsZero() {
		t.Fatalf("create share: %v", err)
	}
	if got, err := s.GetShare(ctx, sh.Token); err != nil || got.UserID != userID {
		t.Fatalf("get share: %v", err)
	}
	if list, _ := s.ListShares(ctx, userID); len(list) != 1 {
		t.Fatalf("list = %d", len(list))
	}
	if err := s.RevokeShare(ctx, "00000000-0000-0000-0000-000000000000", sh.Token); err != ErrNotFound {
		t.Fatalf("revoke by another user: %v", err)
	}
	if err := s.RevokeShare(ctx, userID, sh.Token); err != nil {
		t.Fatal(err)
	}
}
