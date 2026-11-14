package scraper

import (
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

func TestExcludedListing(t *testing.T) {
	excluded := []string{
		"Lot de 3 jeux GameCube", "Zelda Wind Waker - pour pièces", "Zelda Wind Waker boîte vide",
		"Zelda Wind Waker notice seule", "Zelda Wind Waker NTSC", "The Wind Waker version japonaise",
		"Zelda Wind Waker repro cartouche", "Mario Kart x2", "Spiele Konvolut GameCube",
	}
	for _, title := range excluded {
		if !excludedListing(title, "Occasion") {
			t.Errorf("%q should be excluded", title)
		}
	}
	kept := []string{"Zelda The Wind Waker GameCube PAL complet", "Mario Kart Double Dash loose", "Pikmin 2 Nintendo Selects"}
	for _, title := range kept {
		if excludedListing(title, "Occasion") {
			t.Errorf("%q should be kept", title)
		}
	}
	if !excludedListing("Zelda", "For parts or not working") {
		t.Error("for-parts condition should be excluded")
	}
}

func TestClassifyListing(t *testing.T) {
	cases := []struct {
		title, condition string
		want             domain.PriceCondition
	}{
		{"Zelda Wind Waker PAL complet", "Occasion", domain.ConditionCIB},
		{"Zelda Wind Waker loose cartouche seule", "Occasion", domain.ConditionLoose},
		{"Zelda Wind Waker sans boîte", "Used", domain.ConditionLoose},
		{"Zelda Wind Waker", "Neuf", domain.ConditionNew},
		{"Zelda Wind Waker", "Occasion", domain.ConditionCIB},
	}
	for _, c := range cases {
		if got := classifyListing(c.title, c.condition); got != c.want {
			t.Errorf("classifyListing(%q, %q) = %s, want %s", c.title, c.condition, got, c.want)
		}
	}
}
