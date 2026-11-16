package domain

import (
	"strings"
	"testing"
)

func TestValidateSync_Facts(t *testing.T) {
	for _, bad := range []Item{
		{ExternalID: "x", Summary: strings.Repeat("a", MaxTextLen+1)},
		{ExternalID: "x", Developer: strings.Repeat("a", MaxTitleLen+1)},
	} {
		if bad.ValidateSync() == nil {
			t.Errorf("a too long field was accepted")
		}
	}
}

func TestSanitizeFacts(t *testing.T) {
	i := Item{Genres: " N/A ", Developer: "Nintendo", ReleaseYear: 1}
	i.SanitizeFacts()
	if i.Genres != "" || i.Developer != "Nintendo" || i.ReleaseYear != 0 {
		t.Errorf("sanitized = %+v", i)
	}
	j := Item{ReleaseYear: 2003}
	j.SanitizeFacts()
	if j.ReleaseYear != 2003 {
		t.Errorf("a valid year was dropped")
	}
}
