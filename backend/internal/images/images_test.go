package images

import (
	"bytes"
	"context"
	"image"
	"image/png"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/domain"
	"github.com/jplanckeel/thevaultbo/internal/store"
)

func TestPhotoForConsole(t *testing.T) {
	for in, want := range map[string]string{
		"GameCube Indigo|Nintendo GameCube":    "gamecube",
		"Wii U Deluxe 32 Go|Wii U":             "wii-u",
		"Wii Blanche|Nintendo Wii":             "wii",
		"New 3DS XL|Nintendo 3DS":              "3ds",
		"DS Lite Blanche|Nintendo DS":          "ds-lite",
		"Game Boy Advance SP|Game Boy Advance": "gba-sp",
		"Game Boy Advance|Game Boy Advance":    "gba",
		"Game Boy Color Jaune|Game Boy Color":  "gbc",
		"Super Nintendo|Super Nintendo":        "snes",
		"NES|Nintendo Entertainment System":    "nes",
		"PlayStation 2 Fat|Sony PlayStation 2": "ps2",
		"PS3 Slim 120 Go|PlayStation 3":        "ps3-slim",
		"PSP 3004|PlayStation Portable":        "psp",
		"PS Vita|PlayStation Vita":             "ps-vita",
		"PlayStation|PlayStation":              "ps1",
		"Mega-CD II|Sega Mega Drive":           "mega-cd",
		"Mega Drive II|Sega Mega Drive":        "mega-drive",
		"Xbox 360 Elite|Xbox 360":              "xbox-360",
		"Xbox|Microsoft Xbox":                  "xbox",
		"Dreamcast|Sega Dreamcast":             "dreamcast",
	} {
		parts := strings.SplitN(in, "|", 2)
		got, ok := PhotoForConsole(parts[0], parts[1])
		if !ok || got.Key != want {
			t.Errorf("%s: got %q (%v), want %q", in, got.Key, ok, want)
		}
	}
	// Models without a free photo are not given another model's photo.
	for _, in := range [][2]string{{"Xbox Series X", "Xbox Series"}, {"DSi XL", "Nintendo DS"}, {"Vectrex", "Vectrex"}} {
		if got, ok := PhotoForConsole(in[0], in[1]); ok {
			t.Errorf("%v: got %q, want none", in, got.Key)
		}
	}
}

func TestAllowedURL(t *testing.T) {
	for raw, want := range map[string]bool{
		"https://i.ebayimg.com/images/g/x/s-l800.jpg":                    true,
		"https://images.igdb.com/igdb/image/upload/t_cover_big_2x/a.jpg": true,
		"https://commons.wikimedia.org/wiki/Special:FilePath/a.jpg":      true,
		"http://i.ebayimg.com/a.jpg":                                     false,
		"https://i.ebayimg.com:8443/a.jpg":                               false,
		"https://user@i.ebayimg.com/a.jpg":                               false,
		"https://i.ebayimg.com.evil.example/a.jpg":                       false,
		"https://localhost/a.jpg":                                        false,
		"file:///etc/passwd":                                             false,
	} {
		if got := AllowedURL(raw); got != want {
			t.Errorf("%s: %v, want %v", raw, got, want)
		}
	}
}

func pngBytes(t *testing.T) []byte {
	t.Helper()
	var buf bytes.Buffer
	if err := png.Encode(&buf, image.NewRGBA(image.Rect(0, 0, 2, 2))); err != nil {
		t.Fatal(err)
	}
	return buf.Bytes()
}

func TestSaveCover(t *testing.T) {
	dir := t.TempDir()
	cover, err := SaveCover(dir, "console-wii", pngBytes(t))
	if err != nil || !strings.HasPrefix(cover, "/covers/console-wii.png?v=") {
		t.Fatalf("cover %q, err %v", cover, err)
	}
	if _, err := os.Stat(filepath.Join(dir, "covers", "console-wii.png")); err != nil {
		t.Fatal(err)
	}
	if _, err := SaveCover(dir, "x", []byte("<html>")); err != ErrUnsupportedType {
		t.Errorf("html: %v", err)
	}
	if _, err := SaveCover(dir, "../x", pngBytes(t)); err == nil {
		t.Error("path traversal accepted")
	}
}

// itemsStub is an ItemStore holding a fixed list and recording cover updates.
type itemsStub struct {
	store.ItemStore
	items   []domain.Item
	updated map[string]string
}

func (s *itemsStub) List(context.Context) ([]domain.Item, error) { return s.items, nil }
func (s *itemsStub) UpdateIGDB(_ context.Context, id string, d domain.IGDBData) error {
	s.updated[id] = d.CoverURL
	return nil
}

func TestConsolePhotosRun(t *testing.T) {
	dir := t.TempDir()
	// The photo of the model is already stored: no download.
	if _, err := SaveCover(dir, "console-gamecube", pngBytes(t)); err != nil {
		t.Fatal(err)
	}
	s := &itemsStub{updated: map[string]string{}, items: []domain.Item{
		{ID: "logo", Type: domain.ItemTypeConsole, Title: "GameCube", CoverURL: "https://images.igdb.com/igdb/image/upload/t_logo_med/x.png"},
		{ID: "empty", Type: domain.ItemTypeConsole, Title: "GameCube Noire"},
		{ID: "admin", Type: domain.ItemTypeConsole, Title: "GameCube", AdminEdited: true},
		{ID: "own", Type: domain.ItemTypeConsole, Title: "GameCube", CoverURL: "/covers/own.jpg"},
		{ID: "game", Type: domain.ItemTypeGame, Title: "GameCube"},
		{ID: "unknown", Type: domain.ItemTypeConsole, Title: "Vectrex"},
	}}
	n, err := NewConsolePhotos(s, dir).Run(context.Background())
	if err != nil || n != 2 {
		t.Fatalf("updated %d, err %v", n, err)
	}
	if s.updated["logo"] != "/covers/console-gamecube.png" || s.updated["empty"] != "/covers/console-gamecube.png" || len(s.updated) != 2 {
		t.Errorf("updated = %v", s.updated)
	}
}
