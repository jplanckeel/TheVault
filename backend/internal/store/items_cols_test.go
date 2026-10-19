package store

import (
	"strings"
	"testing"
)

func TestPrefixedItemCols_MatchesItemCols(t *testing.T) {
	got := strings.Split(prefixedItemCols("i"), ", ")
	want := strings.Split(itemCols, ",")
	if len(got) != len(want) {
		t.Fatalf("got %d columns, want %d", len(got), len(want))
	}
	for k, c := range want {
		if got[k] != "i."+strings.TrimSpace(c) {
			t.Errorf("column %d = %q, want i.%s", k, got[k], strings.TrimSpace(c))
		}
	}
}
