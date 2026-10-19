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
	}
	return nil
}
