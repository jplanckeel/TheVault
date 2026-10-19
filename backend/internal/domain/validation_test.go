package domain

import (
	"strings"
	"testing"
)

func TestValidCoverURL(t *testing.T) {
	for raw, want := range map[string]bool{
		"":                                   true,
		"https://images.igdb.com/a.jpg":      true,
		"http://example.com/c.png":           true,
		"/covers/1b4e28ba-2fa1.jpg":          true,
		"/covers/../etc/passwd":              false,
		"javascript:alert(1)":                false,
		"data:image/svg+xml;base64,PHN2Zz4=": false,
		"https://":                           false,
		"https://x.com/" + strings.Repeat("a", MaxURLLen): false,
	} {
		if got := ValidCoverURL(raw); got != want {
			t.Errorf("ValidCoverURL(%q) = %v, want %v", raw, got, want)
		}
	}
}

func TestIsUUID(t *testing.T) {
	if !IsUUID("1b4e28ba-2fa1-11d2-883f-0016d3cca427") || IsUUID("../../etc") || IsUUID("1b4e28ba") {
		t.Error("IsUUID returned a wrong result")
	}
}

func TestItem_ValidateSync(t *testing.T) {
	ok := Item{ExternalID: "halo", Type: ItemTypeGame, Title: "Halo"}
	if err := ok.ValidateSync(); err != nil {
		t.Fatalf("valid item rejected: %v", err)
	}
	bad := []Item{
		{},
		{ExternalID: "x", Type: "dvd"},
		{ExternalID: "x", Title: strings.Repeat("t", MaxTitleLen+1)},
		{ExternalID: "x", CoverURL: "javascript:alert(1)"},
	}
	for _, it := range bad {
		if it.ValidateSync() == nil {
			t.Errorf("expected rejection for %+v", it)
		}
	}
}
