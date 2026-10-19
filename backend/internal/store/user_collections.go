package store

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// UserCollectionEntry represents an item in a user's collection.
type UserCollectionEntry struct {
	UserID  string    `json:"user_id"`
	ItemID  string    `json:"item_id"`
	Status  string    `json:"status"` // "owned" or "wishlist"
	AddedAt time.Time `json:"added_at"`
}

// UserCollectionStore defines persistence for user collections.
type UserCollectionStore interface {
	Add(ctx context.Context, userID, itemID, status string) error
	Remove(ctx context.Context, userID, itemID string) error
	ListByUser(ctx context.Context, userID, status string) ([]domain.Item, error)
}

// PostgresUserCollectionStore implements UserCollectionStore.
type PostgresUserCollectionStore struct {
	pool *pgxpool.Pool
}

func NewUserCollectionStore(pool *pgxpool.Pool) *PostgresUserCollectionStore {
	return &PostgresUserCollectionStore{pool: pool}
}

func (s *PostgresUserCollectionStore) Add(ctx context.Context, userID, itemID, status string) error {
	_, err := s.pool.Exec(ctx,
		`INSERT INTO user_collections (user_id, item_id, status) VALUES ($1, $2, $3)
		 ON CONFLICT (user_id, item_id) DO UPDATE SET status = $3`,
		userID, itemID, status,
	)
	return err
}

func (s *PostgresUserCollectionStore) Remove(ctx context.Context, userID, itemID string) error {
	_, err := s.pool.Exec(ctx,
		`DELETE FROM user_collections WHERE user_id = $1 AND item_id = $2`,
		userID, itemID,
	)
	return err
}

func (s *PostgresUserCollectionStore) ListByUser(ctx context.Context, userID, status string) ([]domain.Item, error) {
	rows, err := s.pool.Query(ctx,
		`SELECT `+prefixedItemCols("i")+`
		 FROM user_collections uc
		 JOIN items i ON i.id = uc.item_id
		 WHERE uc.user_id = $1 AND uc.status = $2
		 ORDER BY uc.added_at DESC`,
		userID, status,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []domain.Item
	for rows.Next() {
		var item domain.Item
		if err := scanItem(rows, &item); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	if items == nil {
		items = []domain.Item{}
	}
	return items, rows.Err()
}
