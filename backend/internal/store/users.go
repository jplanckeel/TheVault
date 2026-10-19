package store

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// User represents a registered user.
type User struct {
	ID           string    `json:"id"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	Name         string    `json:"name"`
	IsAdmin      bool      `json:"is_admin"`
	CreatedAt    time.Time `json:"created_at"`
}

// UserStore defines persistence operations for users.
type UserStore interface {
	Create(ctx context.Context, email, passwordHash, name string) (User, error)
	GetByEmail(ctx context.Context, email string) (User, error)
	GetByID(ctx context.Context, id string) (User, error)
	// PromoteAdmins grants the admin role to the given e-mails (bootstrap from ADMIN_EMAILS).
	PromoteAdmins(ctx context.Context, emails []string) (int64, error)
}

// PostgresUserStore implements UserStore.
type PostgresUserStore struct {
	pool *pgxpool.Pool
}

func NewUserStore(pool *pgxpool.Pool) *PostgresUserStore {
	return &PostgresUserStore{pool: pool}
}

func (s *PostgresUserStore) Create(ctx context.Context, email, passwordHash, name string) (User, error) {
	var u User
	err := s.pool.QueryRow(ctx,
		`INSERT INTO users (email, password_hash, name) VALUES ($1, $2, $3)
		 RETURNING id, email, password_hash, name, is_admin, created_at`,
		email, passwordHash, name,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Name, &u.IsAdmin, &u.CreatedAt)
	return u, err
}

func (s *PostgresUserStore) GetByEmail(ctx context.Context, email string) (User, error) {
	var u User
	err := s.pool.QueryRow(ctx,
		`SELECT id, email, password_hash, name, is_admin, created_at FROM users WHERE lower(email) = lower($1)`, email,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Name, &u.IsAdmin, &u.CreatedAt)
	return u, err
}

func (s *PostgresUserStore) GetByID(ctx context.Context, id string) (User, error) {
	var u User
	err := s.pool.QueryRow(ctx,
		`SELECT id, email, password_hash, name, is_admin, created_at FROM users WHERE id = $1`, id,
	).Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Name, &u.IsAdmin, &u.CreatedAt)
	return u, err
}

// PromoteAdmins sets is_admin for the given e-mails (case-insensitive) and returns the number of users promoted.
func (s *PostgresUserStore) PromoteAdmins(ctx context.Context, emails []string) (int64, error) {
	if len(emails) == 0 {
		return 0, nil
	}
	tag, err := s.pool.Exec(ctx,
		`UPDATE users SET is_admin = TRUE WHERE NOT is_admin AND lower(email) = ANY($1)`, emails)
	if err != nil {
		return 0, err
	}
	return tag.RowsAffected(), nil
}
