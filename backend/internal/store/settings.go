package store

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
)

// PostgresSettingsStore implements SettingsStore using pgx.
type PostgresSettingsStore struct {
	pool *pgxpool.Pool
}

// NewSettingsStore creates a new PostgresSettingsStore.
func NewSettingsStore(pool *pgxpool.Pool) *PostgresSettingsStore {
	return &PostgresSettingsStore{pool: pool}
}

// Get returns the value for a given key.
func (s *PostgresSettingsStore) Get(ctx context.Context, key string) (string, error) {
	var value string
	err := s.pool.QueryRow(ctx, `SELECT value FROM settings WHERE key=$1`, key).Scan(&value)
	if err != nil {
		return "", fmt.Errorf("settings get %q: %w", key, err)
	}
	return value, nil
}

// Set inserts or updates a setting.
func (s *PostgresSettingsStore) Set(ctx context.Context, key, value string) error {
	_, err := s.pool.Exec(ctx,
		`INSERT INTO settings (key, value) VALUES ($1, $2) ON CONFLICT (key) DO UPDATE SET value=EXCLUDED.value`,
		key, value,
	)
	if err != nil {
		return fmt.Errorf("settings set %q: %w", key, err)
	}
	return nil
}

// GetAll returns all settings as a map.
func (s *PostgresSettingsStore) GetAll(ctx context.Context) (map[string]string, error) {
	rows, err := s.pool.Query(ctx, `SELECT key, value FROM settings`)
	if err != nil {
		return nil, fmt.Errorf("settings getAll: %w", err)
	}
	defer rows.Close()

	m := make(map[string]string)
	for rows.Next() {
		var k, v string
		if err := rows.Scan(&k, &v); err != nil {
			return nil, fmt.Errorf("settings scan: %w", err)
		}
		m[k] = v
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("settings getAll rows: %w", err)
	}
	return m, nil
}

// compile-time interface satisfaction check
var _ SettingsStore = (*PostgresSettingsStore)(nil)
