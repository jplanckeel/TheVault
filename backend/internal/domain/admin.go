package domain

import (
	"strings"
	"time"
	"unicode"
)

// Sync event sources.
const (
	SourceMobile = "mobile"
	SourceWeb    = "web"
	SourceAdmin  = "admin"
)

// Sync event actions.
const (
	ActionCreated  = "created"
	ActionUpdated  = "updated"
	ActionRejected = "rejected"
	ActionEdited   = "edited"
	ActionDeleted  = "deleted"
	ActionMerged   = "merged"
)

// SyncEvent is one write to the shared catalogue (sync from an app or admin correction).
type SyncEvent struct {
	ID         string    `json:"id"`
	Source     string    `json:"source"`
	Action     string    `json:"action"`
	ItemID     *string   `json:"item_id"`
	ExternalID string    `json:"external_id"`
	Title      string    `json:"title"`
	Actor      string    `json:"actor"`
	Detail     string    `json:"detail"`
	CreatedAt  time.Time `json:"created_at"`
}

// SyncEventFilter narrows a sync event listing.
type SyncEventFilter struct {
	Source string
	Action string
	ItemID string
	Limit  int
}

// SyncSummary counts events per source and action over a period.
type SyncSummary struct {
	Since  time.Time                 `json:"since"`
	Counts map[string]map[string]int `json:"counts"` // source -> action -> count
}

// ItemPatch holds the catalogue fields an admin can correct. Nil fields are left untouched.
type ItemPatch struct {
	Type        *ItemType `json:"type"`
	Title       *string   `json:"title"`
	Platform    *string   `json:"platform"`
	Region      *string   `json:"region"`
	Condition   *string   `json:"condition"`
	CoverURL    *string   `json:"cover_url"`
	IgdbID      *string   `json:"igdb_id"`
	Summary     *string   `json:"summary"`
	Genres      *string   `json:"genres"`
	Developer   *string   `json:"developer"`
	Publisher   *string   `json:"publisher"`
	ReleaseYear *int      `json:"release_year"`
}

// Validate rejects patches that would leave an item unusable.
func (p ItemPatch) Validate() error {
	if p.Title != nil && strings.TrimSpace(*p.Title) == "" {
		return ErrInvalidPatch("title cannot be empty")
	}
	if p.Type != nil && *p.Type != ItemTypeGame && *p.Type != ItemTypeConsole && *p.Type != ItemTypeAccessory {
		return ErrInvalidPatch("type must be game, console or accessory")
	}
	if p.ReleaseYear != nil && (*p.ReleaseYear < 0 || *p.ReleaseYear > 2100) {
		return ErrInvalidPatch("release_year out of range")
	}
	return nil
}

// ErrInvalidPatch is returned by ItemPatch.Validate.
type ErrInvalidPatch string

func (e ErrInvalidPatch) Error() string { return string(e) }

// Data quality issues detected on catalogue items.
const (
	IssueMissingCover    = "missing_cover"
	IssueMissingIGDB     = "missing_igdb"
	IssueMissingPlatform = "missing_platform"
	IssueMissingPrice    = "missing_price"
	IssueDuplicate       = "duplicate"
)

// AdminItem is an item enriched with back-office information.
type AdminItem struct {
	Item
	Issues       []string `json:"issues"`
	DuplicateIDs []string `json:"duplicate_ids"`
	Owners       int      `json:"owners"`
	HasPrice     bool     `json:"has_price"`
}

// DuplicateKey normalises type, platform and title so that "Zelda: Wind Waker"
// and "zelda wind-waker" on the same platform collide.
func DuplicateKey(i Item) string {
	norm := func(s string) string {
		var b strings.Builder
		for _, r := range strings.ToLower(s) {
			if unicode.IsLetter(r) || unicode.IsDigit(r) {
				b.WriteRune(r)
			}
		}
		return b.String()
	}
	return string(i.Type) + "|" + norm(i.Platform) + "|" + norm(i.Title)
}

// BuildAdminItems detects data quality issues on every item.
// priced holds the IDs of items with at least one price snapshot, owners the number of users per item.
func BuildAdminItems(items []Item, priced map[string]bool, owners map[string]int) []AdminItem {
	groups := make(map[string][]string, len(items))
	for _, i := range items {
		key := DuplicateKey(i)
		groups[key] = append(groups[key], i.ID)
	}

	out := make([]AdminItem, 0, len(items))
	for _, i := range items {
		a := AdminItem{Item: i, Issues: []string{}, DuplicateIDs: []string{}, Owners: owners[i.ID], HasPrice: priced[i.ID]}
		if strings.TrimSpace(i.CoverURL) == "" {
			a.Issues = append(a.Issues, IssueMissingCover)
		}
		if i.Type == ItemTypeGame && i.IgdbID == "" {
			a.Issues = append(a.Issues, IssueMissingIGDB)
		}
		if strings.TrimSpace(i.Platform) == "" {
			a.Issues = append(a.Issues, IssueMissingPlatform)
		}
		if !a.HasPrice {
			a.Issues = append(a.Issues, IssueMissingPrice)
		}
		for _, id := range groups[DuplicateKey(i)] {
			if id != i.ID {
				a.DuplicateIDs = append(a.DuplicateIDs, id)
			}
		}
		if len(a.DuplicateIDs) > 0 {
			a.Issues = append(a.Issues, IssueDuplicate)
		}
		out = append(out, a)
	}
	return out
}
