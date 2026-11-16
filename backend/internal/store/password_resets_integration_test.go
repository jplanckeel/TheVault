package store

import (
	"context"
	"errors"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// Runs against a migrated database when TEST_DATABASE_URL is set.
func TestPasswordResetStore_Postgres(t *testing.T) {
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
	email := fmt.Sprintf("reset-%d@test.local", time.Now().UnixNano())
	if err := pool.QueryRow(ctx, `INSERT INTO users (email, password_hash, name) VALUES ($1, 'old', 'T') RETURNING id`, email).Scan(&userID); err != nil {
		t.Fatal(err)
	}
	s := NewPasswordResetStore(pool)
	now := time.Now()
	for _, h := range []string{"a-" + userID, "b-" + userID} {
		if err := s.CreateReset(ctx, userID, h, now.Add(time.Hour)); err != nil {
			t.Fatal(err)
		}
	}
	if err := s.CreateReset(ctx, userID, "expired-"+userID, now.Add(-time.Minute)); err != nil {
		t.Fatal(err)
	}

	if _, err := s.ConsumeReset(ctx, "expired-"+userID, "new"); !errors.Is(err, ErrResetInvalid) {
		t.Errorf("expired link: %v, want ErrResetInvalid", err)
	}
	u, err := s.ConsumeReset(ctx, "a-"+userID, "new")
	if err != nil || u.PasswordHash != "new" || u.Email != email {
		t.Fatalf("consume: %+v, %v", u, err)
	}
	// Used once, and the other pending link of the account stops working too.
	for _, h := range []string{"a-" + userID, "b-" + userID, "unknown"} {
		if _, err := s.ConsumeReset(ctx, h, "again"); !errors.Is(err, ErrResetInvalid) {
			t.Errorf("%s: %v, want ErrResetInvalid", h, err)
		}
	}
}
