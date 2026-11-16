package store

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// VaultStore persists the mobile backups and the public share links.
type VaultStore interface {
	// SaveBackup stores payload as the current backup and as a new version, keeping
	// the latest domain.MaxBackupVersions.
	SaveBackup(ctx context.Context, userID string, payload []byte, itemCount int) (time.Time, error)
	GetBackup(ctx context.Context, userID string) ([]byte, time.Time, error)
	// CurrentBackupCount is the number of items of the current backup (0 when none).
	CurrentBackupCount(ctx context.Context, userID string) (int, error)
	ListBackupVersions(ctx context.Context, userID string) ([]domain.BackupVersion, error)
	// GetBackupVersion returns one of the user's versions, ErrNotFound otherwise.
	GetBackupVersion(ctx context.Context, userID string, id int64) ([]byte, error)
	CreateShare(ctx context.Context, share domain.Share) (domain.Share, error)
	ListShares(ctx context.Context, userID string) ([]domain.Share, error)
	RevokeShare(ctx context.Context, userID, token string) error
	GetShare(ctx context.Context, token string) (domain.Share, error)
}

// PostgresVaultStore implements VaultStore.
type PostgresVaultStore struct {
	pool *pgxpool.Pool
}

// NewVaultStore creates a PostgresVaultStore.
func NewVaultStore(pool *pgxpool.Pool) *PostgresVaultStore {
	return &PostgresVaultStore{pool: pool}
}

// SaveBackup stores the backup of a user as current and as a new version.
func (s *PostgresVaultStore) SaveBackup(ctx context.Context, userID string, payload []byte, itemCount int) (time.Time, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return time.Time{}, fmt.Errorf("save backup: %w", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()
	var at time.Time
	if err := tx.QueryRow(ctx, `
		INSERT INTO user_backups (user_id, payload, updated_at) VALUES ($1, $2, NOW())
		ON CONFLICT (user_id) DO UPDATE SET payload = EXCLUDED.payload, updated_at = NOW()
		RETURNING updated_at`, userID, payload).Scan(&at); err != nil {
		return time.Time{}, fmt.Errorf("save backup: %w", err)
	}
	if _, err := tx.Exec(ctx, `INSERT INTO user_backup_versions (user_id, payload, item_count, created_at) VALUES ($1, $2, $3, $4)`,
		userID, payload, itemCount, at); err != nil {
		return time.Time{}, fmt.Errorf("save backup version: %w", err)
	}
	if _, err := tx.Exec(ctx, `
		DELETE FROM user_backup_versions WHERE user_id = $1 AND id NOT IN (
			SELECT id FROM user_backup_versions WHERE user_id = $1 ORDER BY created_at DESC, id DESC LIMIT $2)`,
		userID, domain.MaxBackupVersions); err != nil {
		return time.Time{}, fmt.Errorf("prune backup versions: %w", err)
	}
	if err := tx.Commit(ctx); err != nil {
		return time.Time{}, fmt.Errorf("save backup: %w", err)
	}
	return at, nil
}

// CurrentBackupCount returns the number of items of the current backup.
func (s *PostgresVaultStore) CurrentBackupCount(ctx context.Context, userID string) (int, error) {
	var n int
	err := s.pool.QueryRow(ctx, `SELECT COALESCE(jsonb_array_length(payload -> 'items'), 0) FROM user_backups WHERE user_id = $1`, userID).Scan(&n)
	if errors.Is(err, pgx.ErrNoRows) {
		return 0, nil
	}
	if err != nil {
		return 0, fmt.Errorf("backup count: %w", err)
	}
	return n, nil
}

// ListBackupVersions returns the saved versions of a user, newest first.
func (s *PostgresVaultStore) ListBackupVersions(ctx context.Context, userID string) ([]domain.BackupVersion, error) {
	rows, err := s.pool.Query(ctx, `SELECT id, item_count, created_at FROM user_backup_versions WHERE user_id = $1 ORDER BY created_at DESC, id DESC`, userID)
	if err != nil {
		return nil, fmt.Errorf("list backup versions: %w", err)
	}
	defer rows.Close()
	versions := make([]domain.BackupVersion, 0)
	for rows.Next() {
		var v domain.BackupVersion
		if err := rows.Scan(&v.ID, &v.ItemCount, &v.CreatedAt); err != nil {
			return nil, fmt.Errorf("backup version scan: %w", err)
		}
		versions = append(versions, v)
	}
	return versions, rows.Err()
}

// GetBackupVersion returns one version of the user's backups.
func (s *PostgresVaultStore) GetBackupVersion(ctx context.Context, userID string, id int64) ([]byte, error) {
	var payload []byte
	err := s.pool.QueryRow(ctx, `SELECT payload FROM user_backup_versions WHERE user_id = $1 AND id = $2`, userID, id).Scan(&payload)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("get backup version: %w", err)
	}
	return payload, nil
}

// GetBackup returns the backup of a user, ErrNotFound when there is none.
func (s *PostgresVaultStore) GetBackup(ctx context.Context, userID string) ([]byte, time.Time, error) {
	var payload []byte
	var at time.Time
	err := s.pool.QueryRow(ctx, `SELECT payload, updated_at FROM user_backups WHERE user_id = $1`, userID).Scan(&payload, &at)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, time.Time{}, ErrNotFound
	}
	if err != nil {
		return nil, time.Time{}, fmt.Errorf("get backup: %w", err)
	}
	return payload, at, nil
}

// CreateShare stores a share link.
func (s *PostgresVaultStore) CreateShare(ctx context.Context, share domain.Share) (domain.Share, error) {
	err := s.pool.QueryRow(ctx, `
		INSERT INTO shares (token, user_id, scope, title, hide_prices) VALUES ($1, $2, $3, $4, $5)
		RETURNING created_at`, share.Token, share.UserID, share.Scope, share.Title, share.HidePrices).Scan(&share.CreatedAt)
	if err != nil {
		return domain.Share{}, fmt.Errorf("create share: %w", err)
	}
	return share, nil
}

// ListShares returns the links of a user, newest first.
func (s *PostgresVaultStore) ListShares(ctx context.Context, userID string) ([]domain.Share, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT token, user_id, scope, title, hide_prices, created_at FROM shares
		WHERE user_id = $1 ORDER BY created_at DESC`, userID)
	if err != nil {
		return nil, fmt.Errorf("list shares: %w", err)
	}
	defer rows.Close()
	shares := make([]domain.Share, 0)
	for rows.Next() {
		var sh domain.Share
		if err := rows.Scan(&sh.Token, &sh.UserID, &sh.Scope, &sh.Title, &sh.HidePrices, &sh.CreatedAt); err != nil {
			return nil, fmt.Errorf("share scan: %w", err)
		}
		shares = append(shares, sh)
	}
	return shares, rows.Err()
}

// RevokeShare deletes a link of the user.
func (s *PostgresVaultStore) RevokeShare(ctx context.Context, userID, token string) error {
	tag, err := s.pool.Exec(ctx, `DELETE FROM shares WHERE token = $1 AND user_id = $2`, token, userID)
	if err != nil {
		return fmt.Errorf("revoke share: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

// GetShare returns a link by token, ErrNotFound when unknown.
func (s *PostgresVaultStore) GetShare(ctx context.Context, token string) (domain.Share, error) {
	var sh domain.Share
	err := s.pool.QueryRow(ctx, `
		SELECT token, user_id, scope, title, hide_prices, created_at FROM shares WHERE token = $1`, token).
		Scan(&sh.Token, &sh.UserID, &sh.Scope, &sh.Title, &sh.HidePrices, &sh.CreatedAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return domain.Share{}, ErrNotFound
	}
	if err != nil {
		return domain.Share{}, fmt.Errorf("get share: %w", err)
	}
	return sh, nil
}

var _ VaultStore = (*PostgresVaultStore)(nil)
