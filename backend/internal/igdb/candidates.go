package igdb

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

// igdbEdition is a game, edition or version returned by a cover search.
type igdbEdition struct {
	Name         string      `json:"name"`
	VersionTitle string      `json:"version_title"`
	Cover        *igdbCover  `json:"cover"`
	Platforms    []igdbNamed `json:"platforms"`
}

// Name identifies the source in the back-office.
func (c *Client) Name() string { return "igdb" }

// Candidates returns the covers of the games, editions and versions matching a game
// title: unlike SearchGame, versions are kept (collector, GOTY, remaster…).
func (c *Client) Candidates(ctx context.Context, item domain.Item) ([]domain.ImageCandidate, error) {
	if item.Type != domain.ItemTypeGame {
		return nil, nil
	}
	var games []igdbEdition
	for _, term := range []string{item.Title, cleanTitle(item.Title)} {
		safe := strings.ReplaceAll(term, `"`, `\"`)
		query := fmt.Sprintf(`fields name,version_title,cover.url,platforms.name; search "%s"; where cover != null; limit 20;`, safe)
		if err := c.query(ctx, igdbGamesURL, query, &games, true); err != nil {
			return nil, err
		}
		if len(games) > 0 || term == cleanTitle(item.Title) {
			break
		}
	}
	out := make([]domain.ImageCandidate, 0, len(games))
	for _, g := range games {
		if g.Cover == nil || g.Cover.URL == "" {
			continue
		}
		u := strings.Replace(g.Cover.URL, "t_thumb", "t_cover_big_2x", 1)
		if strings.HasPrefix(u, "//") {
			u = "https:" + u
		}
		label := g.Name
		if g.VersionTitle != "" {
			label += " — " + g.VersionTitle
		}
		var platforms []string
		for _, p := range g.Platforms {
			platforms = append(platforms, p.Name)
		}
		if len(platforms) > 0 {
			label += " (" + strings.Join(platforms, ", ") + ")"
		}
		out = append(out, domain.ImageCandidate{URL: u, Source: "igdb", Label: label})
	}
	return out, nil
}

// query posts an APIv4 query and decodes the result into out, renewing an expired token once.
func (c *Client) query(ctx context.Context, endpoint, query string, out any, retry bool) error {
	token, err := c.getToken()
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewBufferString(query))
	if err != nil {
		return fmt.Errorf("igdb request: %w", err)
	}
	req.Header.Set("Client-ID", c.clientID)
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Accept", "application/json")
	resp, err := c.http.Do(req)
	if err != nil {
		return fmt.Errorf("igdb query: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusUnauthorized && retry {
		c.mu.Lock()
		c.cachedToken = ""
		c.mu.Unlock()
		return c.query(ctx, endpoint, query, out, false)
	}
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("igdb query: HTTP %d", resp.StatusCode)
	}
	if err := json.NewDecoder(resp.Body).Decode(out); err != nil {
		return fmt.Errorf("igdb decode: %w", err)
	}
	return nil
}
