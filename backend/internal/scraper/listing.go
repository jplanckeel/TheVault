package scraper

import (
	"regexp"
	"strings"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// excludedPatterns match listings that do not price one PAL copy of the item: lots and
// bundles, broken or for-parts items, reproductions, single parts (empty box, manual or
// cover only) and NTSC / import copies.
var excludedPatterns = regexp.MustCompile(`(?i)` + strings.Join([]string{
	`\blots?\b`, `\bbundle\b`, `\bpack de\b`, `\bx\s?[2-9]\b`, `\b[2-9]\s?jeux\b`, `\bcollection de\b`, `\bkonvolut\b`, `\bsammlung\b`,
	`pour pi[eè]ces`, `for parts`, `not working`, `ne fonctionne pas`, `\bhs\b`, `d[ée]fectueu`, `defekt`, `en panne`,
	`\brepro`, `replica`, `\bcopie\b`, `fan ?made`,
	`bo[iî]te vide`, `boite seule`, `bo[iî]te seule`, `empty box`, `box only`, `\bnur ovp\b`, `leerverpackung`,
	`notice seule`, `manuel seul`, `manual only`, `nur anleitung`, `jaquette seule`, `cover only`,
	`\bntsc\b`, `\bjap\b`, `japan`, `japonais`, `\bimport (us|usa|jp)\b`, `version (us|usa|jap|japonaise|am[ée]ricaine)`,
}, "|"))

// excludedListing reports whether a listing must be ignored for the PAL quote.
func excludedListing(title, condition string) bool {
	c := strings.ToLower(condition)
	if strings.Contains(c, "for parts") || strings.Contains(c, "pour pièces") || strings.Contains(c, "pour pieces") {
		return true
	}
	return excludedPatterns.MatchString(title)
}

var (
	looseTitle = regexp.MustCompile(`(?i)\bloose\b|sans bo[iî]te|sans notice|cartouche seule|jeu seul|disque seul|cd seul|cartridge only|disc only|game only|\bohne ovp\b|nur modul|nur spiel`)
	cibTitle   = regexp.MustCompile(`(?i)\bcomplet\b|\bcomplete\b|\bcib\b|\bovp\b|avec notice|boite et notice|bo[iî]te \+ notice|\bin box\b`)
)

// classifyListing picks the condition family of a listing: the eBay condition first for
// new / sealed items, then the title (sellers write « loose », « complet »…).
func classifyListing(title, condition string) domain.PriceCondition {
	cond := mapCondition(condition)
	switch {
	case cond == domain.ConditionNew:
		return cond
	case looseTitle.MatchString(title):
		return domain.ConditionLoose
	case cibTitle.MatchString(title):
		return domain.ConditionCIB
	default:
		return cond
	}
}
