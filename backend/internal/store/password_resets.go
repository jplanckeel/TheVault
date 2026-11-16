package store

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ErrResetInvalid is returned for an unknown, used or expired reset link.
var ErrResetInvalid = errors.New("reset link invalid or expired")

// PasswordResetStore keeps the one-time password reset links (by token hash).
type PasswordResetStore interface {
	// CreateReset records a link for userID, valid until expiresAt.
	CreateReset(ctx context.Context, userID, tokenHash string, expiresAt time.Time) error
	// ConsumeReset sets the new password of the link's user and uses up every pending
	// link of that user. Returns ErrResetInvalid when the link cannot be used.
	ConsumeReset(ctx context.Context, tokenHash, passwordHash string) (User, error)
}

// PostgresPasswordResetStore implements PasswordResetStore.
type PostgresPasswordResetStore struct {
	pool *pgxpool.Pool
}

// NewPasswordResetStore returns a PasswordResetStore backed by Postgres.
func NewPasswordResetStore(pool *pgxpool.Pool) *PostgresPasswordResetStore {
	return &PostgresPasswordResetStore{pool: pool}
}

// CreateReset records a link.
func (s *PostgresPasswordResetStore) CreateReset(ctx context.Context, userID, tokenHash string, expiresAt time.Time) error {
	_, err := s.pool.Exec(ctx,
		`INSERT INTO password_resets (token_hash, user_id, expires_at) VALUES ($1, $2, $3)`, tokenHash, userID, expiresAt)
	if err != nil {
		return fmt.Errorf("create reset: %w", err)
	}
	return nil
}

// ConsumeReset uses a link and changes the password, in one transaction.
func (s *PostgresPasswordResetStore) ConsumeReset(ctx context.Context, tokenHash, passwordHash string) (User, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return User{}, fmt.Errorf("begin: %w", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var userID string
	err = tx.QueryRow(ctx, `
		UPDATE password_resets SET used_at = NOW()
		WHERE token_hash = $1 AND used_at IS NULL AND expires_at > NOW()
		RETURNING user_id`, tokenHash).Scan(&userID)
	if errors.Is(err, pgx.ErrNoRows) {
		return User{}, ErrResetInvalid
	}
	if err != nil {
		return User{}, fmt.Errorf("use reset: %w", err)
	}
	var u User
	err = tx.QueryRow(ctx, `
		UPDATE users SET password_hash = $2 WHERE id = $1
		RETURNING id, email, password_hash, name, is_admin, created_at`, userID, passwordHash,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Name, &u.IsAdmin, &u.CreatedAt)
	if err != nil {
		return User{}, fmt.Errorf("set password: %w", err)
	}
	// Any other link sent for this account stops working.
	if _, err := tx.Exec(ctx, `UPDATE password_resets SET used_at = NOW() WHERE user_id = $1 AND used_at IS NULL`, userID); err != nil {
		return User{}, fmt.Errorf("revoke resets: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return User{}, fmt.Errorf("commit: %w", err)
	}
	return u, nil
}

var _ PasswordResetStore = (*PostgresPasswordResetStore)(nil)
