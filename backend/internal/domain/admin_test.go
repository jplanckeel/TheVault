package domain

import (
	"reflect"
	"testing"
)

func TestDuplicateKey_IgnoresCaseAndPunctuation(t *testing.T) {
	a := Item{Type: ItemTypeGame, Platform: "Nintendo GameCube", Title: "Zelda: The Wind Waker"}
	b := Item{Type: ItemTypeGame, Platform: "nintendo gamecube", Title: "zelda the wind-waker"}
	if DuplicateKey(a) != DuplicateKey(b) {
		t.Fatalf("expected same key, got %q and %q", DuplicateKey(a), DuplicateKey(b))
	}
	c := b
	c.Platform = "Wii"
	if DuplicateKey(a) == DuplicateKey(c) {
		t.Fatal("different platforms must not collide")
	}
}

func TestBuildAdminItems_DetectsIssues(t *testing.T) {
	items := []Item{
		{ID: "1", Type: ItemTypeGame, Title: "Halo", Platform: "Xbox", CoverURL: "c", IgdbID: "9"},
		{ID: "2", Type: ItemTypeGame, Title: "halo", Platform: "XBOX"},
		{ID: "3", Type: ItemTypeConsole, Title: "PS2", Platform: ""},
	}
	got := BuildAdminItems(items, map[string]bool{"1": true}, map[string]int{"1": 4})

	if len(got[0].Issues) != 1 || got[0].Issues[0] != IssueDuplicate {
		t.Errorf("item 1 issues = %v, want [duplicate]", got[0].Issues)
	}
	if got[0].Owners != 4 || !got[0].HasPrice {
		t.Errorf("item 1 owners/price not propagated: %+v", got[0])
	}
	want2 := []string{IssueMissingCover, IssueMissingIGDB, IssueMissingPrice, IssueDuplicate}
	if !reflect.DeepEqual(got[1].Issues, want2) {
		t.Errorf("item 2 issues = %v, want %v", got[1].Issues, want2)
	}
	if !reflect.DeepEqual(got[1].DuplicateIDs, []string{"1"}) {
		t.Errorf("item 2 duplicates = %v", got[1].DuplicateIDs)
	}
	want3 := []string{IssueMissingCover, IssueMissingPlatform, IssueMissingPrice}
	if !reflect.DeepEqual(got[2].Issues, want3) {
		t.Errorf("console issues = %v, want %v (no IGDB check for consoles)", got[2].Issues, want3)
	}
}

func TestItemPatch_Validate(t *testing.T) {
	empty := "  "
	bad := ItemType("dvd")
	year := 3000
	for name, p := range map[string]ItemPatch{"empty title": {Title: &empty}, "bad type": {Type: &bad}, "bad year": {ReleaseYear: &year}} {
		if p.Validate() == nil {
			t.Errorf("%s: expected an error", name)
		}
	}
	title := "Ok"
	if err := (ItemPatch{Title: &title}).Validate(); err != nil {
		t.Errorf("valid patch rejected: %v", err)
	}
}
