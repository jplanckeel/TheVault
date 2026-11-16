package images

import (
	"context"
	"fmt"
	"log/slog"
	"net/url"
	"regexp"
	"strings"
	"sync"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

// ConsolePhoto is a Wikimedia Commons photo of a console model.
type ConsolePhoto struct {
	// Key names the stored file (covers/console-<key>.<ext>), shared by every console of the model.
	Key string
	// File is the Commons file name. Every file below was checked on Commons to be in the
	// public domain or CC0 (no attribution required); keep it that way when adding one.
	File string
}

// URL returns a 800 px wide rendition of the photo (redirects to upload.wikimedia.org).
func (p ConsolePhoto) URL() string {
	return "https://commons.wikimedia.org/wiki/Special:FilePath/" + url.PathEscape(p.File) + "?width=800"
}

// PageURL returns the Commons page of the photo (author, licence).
func (p ConsolePhoto) PageURL() string {
	return "https://commons.wikimedia.org/wiki/File:" + url.PathEscape(strings.ReplaceAll(p.File, " ", "_"))
}

type consoleRule struct {
	match *regexp.Regexp
	photo ConsolePhoto
}

// consoleRules are tried in order against "<title> <platform>": the most specific models
// first (Wii U before Wii, 3DS before DS, Mega-CD before Mega Drive…). A rule without a
// file marks a model with no free photo, so a more generic rule does not take it.
var consoleRules = []consoleRule{
	{regexp.MustCompile(`xbox series`), ConsolePhoto{}},
	{regexp.MustCompile(`xbox one`), ConsolePhoto{"xbox-one", "Microsoft-Xbox-One-Console-Set-wKinect.jpg"}},
	{regexp.MustCompile(`xbox 360`), ConsolePhoto{"xbox-360", "Xbox-360-Pro-wController.jpg"}},
	{regexp.MustCompile(`\bxbox\b`), ConsolePhoto{"xbox", "Xbox-console.jpg"}},
	{regexp.MustCompile(`\bps5\b|playstation 5`), ConsolePhoto{"ps5", "Black and white Playstation 5 base edition with controller.png"}},
	{regexp.MustCompile(`\bps4\b|playstation 4`), ConsolePhoto{"ps4", "PS4-Console-wDS4.jpg"}},
	{regexp.MustCompile(`(\bps3\b|playstation 3).*slim|slim.*(\bps3\b|playstation 3)`), ConsolePhoto{"ps3-slim", "PS3-slim-console.png"}},
	{regexp.MustCompile(`\bps3\b|playstation 3`), ConsolePhoto{"ps3", "Sony-PlayStation-3-CECHA01-wController-L.jpg"}},
	{regexp.MustCompile(`\bps2\b|playstation 2`), ConsolePhoto{"ps2", "PS2-Fat-Console-Set.jpg"}},
	{regexp.MustCompile(`\bvita\b`), ConsolePhoto{"ps-vita", "PlayStation-Vita-1101-FL.jpg"}},
	{regexp.MustCompile(`\bpsp\b|playstation portable`), ConsolePhoto{"psp", "Psp-1000.jpg"}},
	{regexp.MustCompile(`\bps1\b|\bpsx\b|\bps ?one\b|playstation`), ConsolePhoto{"ps1", "PSX-Console-wController.jpg"}},
	{regexp.MustCompile(`wii u`), ConsolePhoto{"wii-u", "Nintendo-Wii-U-Console-FL.jpg"}},
	{regexp.MustCompile(`\bwii\b`), ConsolePhoto{"wii", "Wii-console.jpg"}},
	{regexp.MustCompile(`switch 2`), ConsolePhoto{}},
	{regexp.MustCompile(`switch`), ConsolePhoto{"switch", "Nintendo-Switch-wJoyCons-BlRd-Standing-FL.jpg"}},
	{regexp.MustCompile(`gamecube|game cube`), ConsolePhoto{"gamecube", "GameCube-Set.jpg"}},
	{regexp.MustCompile(`nintendo 64|\bn64\b`), ConsolePhoto{"n64", "N64-Console-Set.png"}},
	{regexp.MustCompile(`virtual boy`), ConsolePhoto{"virtual-boy", "Virtual-Boy-wController.jpg"}},
	{regexp.MustCompile(`game ?boy micro`), ConsolePhoto{}},
	{regexp.MustCompile(`advance sp|\bgba sp\b`), ConsolePhoto{"gba-sp", "Gameboy-Advance-SP-Mk2.jpg"}},
	{regexp.MustCompile(`game ?boy advance|\bgba\b`), ConsolePhoto{"gba", "Nintendo-Game-Boy-Advance-Purple-FL.jpg"}},
	{regexp.MustCompile(`game ?boy colou?r|\bgbc\b`), ConsolePhoto{"gbc", "Nintendo-Game-Boy-Color-FL.jpg"}},
	{regexp.MustCompile(`game ?boy|\bgb\b`), ConsolePhoto{"game-boy", "Game-Boy-FL.jpg"}},
	{regexp.MustCompile(`\b(new )?(nintendo )?(2|3)ds\b`), ConsolePhoto{"3ds", "Nintendo-3DS-AquaOpen.jpg"}},
	{regexp.MustCompile(`\bds ?lite\b`), ConsolePhoto{"ds-lite", "Nintendo-DS-Lite-w-stylus.png"}},
	{regexp.MustCompile(`\bdsi\b`), ConsolePhoto{}},
	{regexp.MustCompile(`nintendo ds|\bds\b`), ConsolePhoto{"ds", "Nintendo-DS-Fat-Blue.jpg"}},
	{regexp.MustCompile(`super nintendo|\bsnes\b|super famicom`), ConsolePhoto{"snes", "Nintendo-Super-Famicom-Set-FL.jpg"}},
	{regexp.MustCompile(`\bnes\b|nintendo entertainment system|famicom`), ConsolePhoto{"nes", "NES-Console-Set.jpg"}},
	{regexp.MustCompile(`mega[ -]?cd|sega cd`), ConsolePhoto{"mega-cd", "Sega-CD-Model1-Set.jpg"}},
	{regexp.MustCompile(`\b32x\b`), ConsolePhoto{"32x", "Sega-Genesis-Model2-32X.jpg"}},
	{regexp.MustCompile(`mega ?drive|genesis`), ConsolePhoto{"mega-drive", "Sega-Mega-Drive-EU-Mk1-wController-FL.jpg"}},
	{regexp.MustCompile(`master system`), ConsolePhoto{"master-system", "Sega-Master-System-Set.jpg"}},
	{regexp.MustCompile(`saturn`), ConsolePhoto{"saturn", "Sega-Saturn-Console-Set-Mk1.png"}},
	{regexp.MustCompile(`dreamcast`), ConsolePhoto{"dreamcast", "Sega-Dreamcast-Console-FL.jpg"}},
	{regexp.MustCompile(`game gear`), ConsolePhoto{"game-gear", "Game-Gear-Handheld.jpg"}},
	{regexp.MustCompile(`atari 2600|\bvcs\b`), ConsolePhoto{"atari-2600", "Atari-2600-Wood-4Sw-Set.jpg"}},
}

// PhotoForConsole returns the Commons photo matching a console title and platform.
func PhotoForConsole(title, platform string) (ConsolePhoto, bool) {
	text := strings.ToLower(title + " " + platform)
	for _, r := range consoleRules {
		if r.match.MatchString(text) {
			return r.photo, r.photo.File != ""
		}
	}
	return ConsolePhoto{}, false
}

// needsConsolePhoto reports whether a console still shows no picture or the IGDB
// platform logo, and was not given a cover in the back-office.
func needsConsolePhoto(item domain.Item) bool {
	if item.Type != domain.ItemTypeConsole || item.AdminEdited {
		return false
	}
	return item.CoverURL == "" || strings.Contains(item.CoverURL, "images.igdb.com")
}

// ConsolePhotosResult sums up a run of the console photo job.
type ConsolePhotosResult struct {
	// Updated consoles now show the photo of their model.
	Updated int `json:"updated"`
	// Failed consoles whose photo could not be downloaded (network, Wikimedia).
	Failed int `json:"failed"`
	// Unmatched consoles whose model has no free photo (or is not recognised).
	Unmatched int `json:"unmatched"`
}

// ConsolePhotos replaces the missing pictures and IGDB logos of consoles with a photo
// of the model. Each photo is downloaded once and shared by every console of the model.
// Runs one at a time (hourly loop and back-office button).
type ConsolePhotos struct {
	items     store.ItemStore
	importer  *Importer
	uploadDir string
	mu        sync.Mutex
}

// NewConsolePhotos returns the console photo job.
func NewConsolePhotos(items store.ItemStore, uploadDir string) *ConsolePhotos {
	return &ConsolePhotos{items: items, importer: NewImporter(uploadDir), uploadDir: uploadDir}
}

// Run applies the photos to the consoles that need one.
func (c *ConsolePhotos) Run(ctx context.Context) (ConsolePhotosResult, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	var res ConsolePhotosResult
	all, err := c.items.List(ctx)
	if err != nil {
		return res, fmt.Errorf("list items: %w", err)
	}
	stored := map[string]string{}
	for _, item := range all {
		if !needsConsolePhoto(item) {
			continue
		}
		photo, ok := PhotoForConsole(item.Title, item.Platform)
		if !ok {
			res.Unmatched++
			continue
		}
		cover, err := c.cover(ctx, photo, stored)
		if err != nil {
			slog.Warn("console photo", "key", photo.Key, "err", err)
			res.Failed++
			continue
		}
		if err := c.items.UpdateIGDB(ctx, item.ID, domain.IGDBData{CoverURL: cover}); err != nil {
			return res, fmt.Errorf("update %s: %w", item.ID, err)
		}
		res.Updated++
	}
	return res, nil
}

// cover returns the stored photo of a model, downloading it the first time.
func (c *ConsolePhotos) cover(ctx context.Context, photo ConsolePhoto, stored map[string]string) (string, error) {
	name := "console-" + photo.Key
	if cover, ok := stored[name]; ok {
		return cover, nil
	}
	cover, ok := existingCover(c.uploadDir, name)
	if !ok {
		var err error
		if cover, err = c.importer.Import(ctx, name, photo.URL()); err != nil {
			return "", err
		}
	}
	stored[name] = cover
	return cover, nil
}
