// Package igdb provides a client for the IGDB game database API (Twitch OAuth2).
package igdb

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

const (
	twitchTokenURL = "https://id.twitch.tv/oauth2/token"
	igdbGamesURL   = "https://api.igdb.com/v4/games"
)

// Client is an IGDB API client backed by Twitch OAuth2.
type Client struct {
	clientID     string
	clientSecret string
	http         *http.Client

	mu          sync.Mutex
	cachedToken string
}

// New creates a new IGDB Client.
func New(clientID, clientSecret string) *Client {
	return &Client{
		clientID:     clientID,
		clientSecret: clientSecret,
		http:         &http.Client{Timeout: 10 * time.Second},
	}
}

// twitchTokenResp is the Twitch OAuth2 token response.
type twitchTokenResp struct {
	AccessToken string `json:"access_token"`
}

// igdbGame is the raw IGDB game response.
type igdbGame struct {
	ID                int64               `json:"id"`
	FirstReleaseDate  int64               `json:"first_release_date"`
	Cover             *igdbCover          `json:"cover"`
	Genres            []igdbNamed         `json:"genres"`
	Summary           string              `json:"summary"`
	InvolvedCompanies []igdbInvolvedComp  `json:"involved_companies"`
}

type igdbCover struct {
	URL string `json:"url"`
}

type igdbNamed struct {
	Name string `json:"name"`
}

type igdbInvolvedComp struct {
	Developer bool       `json:"developer"`
	Publisher bool       `json:"publisher"`
	Company   *igdbNamed `json:"company"`
}

// getToken fetches and caches a Twitch OAuth2 access token.
func (c *Client) getToken() (string, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.cachedToken != "" {
		return c.cachedToken, nil
	}

	tokenURL := fmt.Sprintf("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",
		twitchTokenURL,
		url.QueryEscape(c.clientID),
		url.QueryEscape(c.clientSecret),
	)

	resp, err := c.http.Post(tokenURL, "", nil)
	if err != nil {
		return "", fmt.Errorf("igdb token request: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("igdb token: HTTP %d", resp.StatusCode)
	}

	var t twitchTokenResp
	if err := json.NewDecoder(resp.Body).Decode(&t); err != nil {
		return "", fmt.Errorf("igdb token decode: %w", err)
	}
	c.cachedToken = t.AccessToken
	return c.cachedToken, nil
}

// SearchGame queries IGDB for the best match for a game title and returns its metadata.
// Returns nil IGDBData (no error) when no result is found.
func (c *Client) SearchGame(title string) (*domain.IGDBData, error) {
	token, err := c.getToken()
	if err != nil {
		return nil, err
	}

	// Escape double-quotes in the title to avoid breaking the IGDB query syntax.
	safeTitle := strings.ReplaceAll(title, `"`, `\"`)
	query := fmt.Sprintf(
		`fields id,first_release_date,cover.url,genres.name,summary,involved_companies.company.name,involved_companies.developer,involved_companies.publisher;`+
			`search "%s"; where version_parent = null; limit 1;`,
		safeTitle,
	)

	req, err := http.NewRequest(http.MethodPost, igdbGamesURL, bytes.NewBufferString(query))
	if err != nil {
		return nil, fmt.Errorf("igdb request: %w", err)
	}
	req.Header.Set("Client-ID", c.clientID)
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Accept", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, fmt.Errorf("igdb search: %w", err)
	}
	defer resp.Body.Close()

	// If token expired (401), clear cache and retry once.
	if resp.StatusCode == http.StatusUnauthorized {
		c.mu.Lock()
		c.cachedToken = ""
		c.mu.Unlock()
		return c.SearchGame(title)
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("igdb search: HTTP %d", resp.StatusCode)
	}

	var games []igdbGame
	if err := json.NewDecoder(resp.Body).Decode(&games); err != nil {
		return nil, fmt.Errorf("igdb decode: %w", err)
	}
	if len(games) == 0 {
		return nil, nil
	}

	g := games[0]
	data := &domain.IGDBData{
		IgdbID:  fmt.Sprintf("%d", g.ID),
		Summary: g.Summary,
	}

	if g.FirstReleaseDate != 0 {
		data.ReleaseYear = int(time.Unix(g.FirstReleaseDate, 0).UTC().Year())
	}

	if g.Cover != nil && g.Cover.URL != "" {
		// IGDB URLs: //images.igdb.com/igdb/image/upload/t_thumb/xxxx.jpg
		// → https://images.igdb.com/igdb/image/upload/t_cover_big/xxxx.jpg
		coverURL := strings.Replace(g.Cover.URL, "t_thumb", "t_cover_big", 1)
		if strings.HasPrefix(coverURL, "//") {
			coverURL = "https:" + coverURL
		}
		data.CoverURL = coverURL
	}

	genres := make([]string, 0, len(g.Genres))
	for _, gen := range g.Genres {
		if gen.Name != "" {
			genres = append(genres, gen.Name)
		}
	}
	data.Genres = strings.Join(genres, ", ")

	for _, ic := range g.InvolvedCompanies {
		if ic.Company == nil {
			continue
		}
		if ic.Developer && data.Developer == "" {
			data.Developer = ic.Company.Name
		}
		if ic.Publisher && data.Publisher == "" {
			data.Publisher = ic.Company.Name
		}
	}

	return data, nil
}
