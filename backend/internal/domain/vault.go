package domain

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"
)

// Share scopes.
const (
	ShareCollection = "collection"
	ShareWishlist   = "wishlist"
)

// Limits of the backup and share endpoints.
const (
	MaxBackupItems = 20000
	MaxShareTitle  = 80
)

// Share is a read-only public link to a user's collection or wishlist.
type Share struct {
	Token      string    `json:"token"`
	UserID     string    `json:"-"`
	Scope      string    `json:"scope"`
	Title      string    `json:"title"`
	HidePrices bool      `json:"hide_prices"`
	CreatedAt  time.Time `json:"created_at"`
}

// ValidateShare checks a share request.
func ValidateShare(scope, title string) error {
	if scope != ShareCollection && scope != ShareWishlist {
		return fmt.Errorf("scope must be collection or wishlist")
	}
	if tooLong(title, MaxShareTitle) {
		return fmt.Errorf("title is too long")
	}
	return nil
}

// BackupItem is the part of a backed-up item that may be shown publicly. Notes, purchase
// prices, storage places and borrowers are never read from the backup.
type BackupItem struct {
	Kind         string   `json:"kind"`
	Title        string   `json:"title"`
	Platform     string   `json:"platform"`
	Status       string   `json:"status"`
	CoverURL     string   `json:"coverUrl"`
	Completeness string   `json:"completeness"`
	Edition      string   `json:"edition"`
	Value        *float64 `json:"value"`
}

// ValidateBackup checks that a backup is a JSON object with an items array.
func ValidateBackup(payload []byte) (int, error) {
	var doc struct {
		Items []json.RawMessage `json:"items"`
	}
	if err := json.Unmarshal(payload, &doc); err != nil {
		return 0, fmt.Errorf("backup must be a JSON object")
	}
	if doc.Items == nil {
		return 0, fmt.Errorf("backup must contain items")
	}
	if len(doc.Items) > MaxBackupItems {
		return 0, fmt.Errorf("at most %d items", MaxBackupItems)
	}
	return len(doc.Items), nil
}

// BackupVersion is one saved backup of a user.
type BackupVersion struct {
	ID        int64     `json:"id"`
	ItemCount int       `json:"item_count"`
	CreatedAt time.Time `json:"created_at"`
}

// Backup history rules.
const (
	// MaxBackupVersions is how many backups are kept per user.
	MaxBackupVersions = 10
	// ShrinkGuardMin is the size from which a shrinking backup needs a confirmation.
	ShrinkGuardMin = 10
)

// IsSuspiciousShrink reports whether replacing a backup of previous items by one of next
// items loses more than half of them (an empty or new phone), which needs confirming.
func IsSuspiciousShrink(previous, next int) bool {
	return previous >= ShrinkGuardMin && next*2 < previous
}

// PublicView is what a share link shows.
type PublicView struct {
	Title     string       `json:"title"`
	Scope     string       `json:"scope"`
	UpdatedAt time.Time    `json:"updated_at"`
	Count     int          `json:"count"`
	Total     *float64     `json:"total"`
	Items     []BackupItem `json:"items"`
}

// BuildPublicView keeps the items of the share scope (owned or wishlist) and strips the
// values when prices are hidden.
func BuildPublicView(share Share, payload []byte, updatedAt time.Time) (PublicView, error) {
	var doc struct {
		Items []BackupItem `json:"items"`
	}
	if err := json.Unmarshal(payload, &doc); err != nil {
		return PublicView{}, fmt.Errorf("decode backup: %w", err)
	}
	want := "OWNED"
	if share.Scope == ShareWishlist {
		want = "WISHLIST"
	}
	view := PublicView{Title: share.Title, Scope: share.Scope, UpdatedAt: updatedAt, Items: []BackupItem{}}
	total := 0.0
	for _, it := range doc.Items {
		if !strings.EqualFold(it.Status, want) {
			continue
		}
		if share.HidePrices {
			it.Value = nil
		} else if it.Value != nil {
			total += *it.Value
		}
		view.Items = append(view.Items, it)
	}
	view.Count = len(view.Items)
	if !share.HidePrices {
		view.Total = &total
	}
	return view, nil
}
