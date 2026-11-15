package domain

import (
	"fmt"
	"net/url"
	"regexp"
	"strings"
	"unicode/utf8"
)

// Field limits for catalogue writes.
const (
	MaxTitleLen      = 300
	MaxShortFieldLen = 120
	MaxTextLen       = 5000
	MaxURLLen        = 2048
)

var uuidPattern = regexp.MustCompile(`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`)

// IsUUID reports whether s is a canonical UUID (used to build file names safely).
func IsUUID(s string) bool { return uuidPattern.MatchString(s) }

// ValidCoverURL accepts empty values, absolute http(s) URLs and uploaded covers (/covers/…).
// It rejects javascript:, data: and other schemes that clients would render as images.
func ValidCoverURL(raw string) bool {
	if raw == "" {
		return true
	}
	if len(raw) > MaxURLLen {
		return false
	}
	if strings.HasPrefix(raw, "/covers/") && !strings.Contains(raw, "..") {
		return true
	}
	u, err := url.Parse(raw)
	return err == nil && (u.Scheme == "https" || u.Scheme == "http") && u.Host != ""
}

func tooLong(s string, max int) bool { return utf8.RuneCountInString(s) > max }

// ValidateSync checks an item sent by the apps (POST /items).
func (i Item) ValidateSync() error {
	switch {
	case strings.TrimSpace(i.ExternalID) == "":
		return fmt.Errorf("external_id is required")
	case tooLong(i.ExternalID, MaxTitleLen):
		return fmt.Errorf("external_id is too long")
	case i.Type != "" && i.Type != ItemTypeGame && i.Type != ItemTypeConsole && i.Type != ItemTypeAccessory:
		return fmt.Errorf("type must be game, console or accessory")
	case tooLong(i.Title, MaxTitleLen):
		return fmt.Errorf("title is too long")
	case tooLong(i.Platform, MaxShortFieldLen), tooLong(i.Region, MaxShortFieldLen),
		tooLong(i.Condition, MaxShortFieldLen), tooLong(i.Status, MaxShortFieldLen):
		return fmt.Errorf("a field is too long")
	case !ValidCoverURL(i.CoverURL):
		return fmt.Errorf("cover_url must be an http(s) URL")
	case tooLong(i.Summary, MaxTextLen), tooLong(i.Genres, MaxTitleLen),
		tooLong(i.Developer, MaxTitleLen), tooLong(i.Publisher, MaxTitleLen):
		return fmt.Errorf("a field is too long")
	}
	return nil
}

// SanitizeFacts drops the catalogue facts an app could not know: placeholder « N/A »
// values and impossible years. A sync is never rejected for them.
func (i *Item) SanitizeFacts() {
	clean := func(s string) string {
		s = strings.TrimSpace(s)
		if strings.EqualFold(s, "N/A") {
			return ""
		}
		return s
	}
	i.Summary, i.Genres = clean(i.Summary), clean(i.Genres)
	i.Developer, i.Publisher = clean(i.Developer), clean(i.Publisher)
	if i.ReleaseYear < MinReleaseYear || i.ReleaseYear > MaxReleaseYear {
		i.ReleaseYear = 0
	}
}

// Bounds of a release year sent by an app (0 means unknown).
const (
	MinReleaseYear = 1950
	MaxReleaseYear = 2100
)

// Limits of the price endpoints used by the apps.
const (
	MaxQuoteIDs      = 2000
	DefaultQuoteDays = 30
	MaxHistoryDays   = 730
)

// ValidateExternalIDs checks the external IDs of a quotes request.
func ValidateExternalIDs(ids []string) error {
	switch {
	case len(ids) == 0:
		return fmt.Errorf("external_ids is required")
	case len(ids) > MaxQuoteIDs:
		return fmt.Errorf("at most %d external_ids", MaxQuoteIDs)
	}
	for _, id := range ids {
		if strings.TrimSpace(id) == "" || tooLong(id, MaxTitleLen) {
			return fmt.Errorf("invalid external_id")
		}
	}
	return nil
}

// ClampDays bounds a `days` query parameter to [1, MaxHistoryDays], using def when unset.
func ClampDays(days, def int) int {
	switch {
	case days <= 0:
		return def
	case days > MaxHistoryDays:
		return MaxHistoryDays
	}
	return days
}
