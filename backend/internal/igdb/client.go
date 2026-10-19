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
	twitchTokenURL    = "https://id.twitch.tv/oauth2/token"
	igdbGamesURL      = "https://api.igdb.com/v4/games"
	igdbPlatformsURL  = "https://api.igdb.com/v4/platforms"
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

// search executes a raw IGDB query string and returns the decoded game list.
// It handles 401 token expiry with a single retry.
func (c *Client) search(query string) ([]igdbGame, error) {
	token, err := c.getToken()
	if err != nil {
		return nil, err
	}

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

	if resp.StatusCode == http.StatusUnauthorized {
		c.mu.Lock()
		c.cachedToken = ""
		c.mu.Unlock()
		return c.search(query)
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("igdb search: HTTP %d", resp.StatusCode)
	}

	var games []igdbGame
	if err := json.NewDecoder(resp.Body).Decode(&games); err != nil {
		return nil, fmt.Errorf("igdb decode: %w", err)
	}
	return games, nil
}
// igdbQuery builds an IGDB APIv4 query for the given search term.
// No version_parent filter so remasters and ports are included.
func igdbQuery(term string) string {
	safe := strings.ReplaceAll(term, `"`, `\"`)
	return fmt.Sprintf(
		`fields id,first_release_date,cover.url,genres.name,summary,`+
			`involved_companies.company.name,involved_companies.developer,involved_companies.publisher;`+
			`search "%s"; limit 1;`,
		safe,
	)
}

// cleanTitle strips common noise from a game title so a fallback search
// has a better chance of matching (e.g. "Zelda: Wind Waker HD (GCN)" → "Zelda Wind Waker HD").
func cleanTitle(title string) string {
	// Remove anything in parentheses or brackets (region tags, editions…)
	re := strings.NewReplacer("(", " ", ")", " ", "[", " ", "]", " ")
	cleaned := re.Replace(title)
	// Collapse extra spaces
	fields := strings.Fields(cleaned)
	return strings.Join(fields, " ")
}

// SearchGame queries IGDB for the best match for a game title.
// Returns nil IGDBData (no error) when no result is found after all attempts.
func (c *Client) SearchGame(title string) (*domain.IGDBData, error) {
	// Pass 1: exact title
	games, err := c.search(igdbQuery(title))
	if err != nil {
		return nil, err
	}

	// Pass 2: cleaned title (removes parentheses / region codes)
	if len(games) == 0 {
		cleaned := cleanTitle(title)
		if cleaned != title {
			games, err = c.search(igdbQuery(cleaned))
			if err != nil {
				return nil, err
			}
		}
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
		// → https://images.igdb.com/igdb/image/upload/t_cover_big_2x/xxxx.jpg
		coverURL := strings.Replace(g.Cover.URL, "t_thumb", "t_cover_big_2x", 1)
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

// SearchGameResult is a single search result returned to the mobile app.
type SearchGameResult struct {
	ID          string   `json:"id"`
	Title       string   `json:"title"`
	ReleaseYear int      `json:"release_year,omitempty"`
	Platforms   []string `json:"platforms,omitempty"`
	Genres      []string `json:"genres,omitempty"`
	CoverURL    string   `json:"cover_url,omitempty"`
	Summary     string   `json:"summary,omitempty"`
	Developer   string   `json:"developer,omitempty"`
	Publisher   string   `json:"publisher,omitempty"`
}

// igdbSearchGame extends igdbGame with name and platforms for search results.
type igdbSearchGame struct {
	ID                int64              `json:"id"`
	Name              string             `json:"name"`
	FirstReleaseDate  int64              `json:"first_release_date"`
	Cover             *igdbCover         `json:"cover"`
	Genres            []igdbNamed        `json:"genres"`
	Platforms         []igdbNamed        `json:"platforms"`
	Summary           string             `json:"summary"`
	InvolvedCompanies []igdbInvolvedComp `json:"involved_companies"`
}

// SearchGames queries IGDB and returns multiple results for a search term.
func (c *Client) SearchGames(term string) ([]SearchGameResult, error) {
	safe := strings.ReplaceAll(term, `"`, `\"`)
	query := fmt.Sprintf(
		`fields name,first_release_date,cover.url,genres.name,platforms.name,summary,`+
			`involved_companies.company.name,involved_companies.developer,involved_companies.publisher;`+
			`search "%s"; where version_parent = null; limit 15;`, safe)

	token, err := c.getToken()
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest(http.MethodPost, igdbGamesURL, bytes.NewBufferString(query))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Client-ID", c.clientID)
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Accept", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized {
		c.mu.Lock()
		c.cachedToken = ""
		c.mu.Unlock()
		return c.SearchGames(term)
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("igdb search: HTTP %d", resp.StatusCode)
	}

	var games []igdbSearchGame
	if err := json.NewDecoder(resp.Body).Decode(&games); err != nil {
		return nil, err
	}

	results := make([]SearchGameResult, 0, len(games))
	for _, g := range games {
		r := SearchGameResult{
			ID:      fmt.Sprintf("%d", g.ID),
			Title:   g.Name,
			Summary: g.Summary,
		}
		if g.FirstReleaseDate != 0 {
			r.ReleaseYear = int(time.Unix(g.FirstReleaseDate, 0).UTC().Year())
		}
		if g.Cover != nil && g.Cover.URL != "" {
			url := strings.Replace(g.Cover.URL, "t_thumb", "t_cover_big_2x", 1)
			if strings.HasPrefix(url, "//") {
				url = "https:" + url
			}
			r.CoverURL = url
		}
		for _, gen := range g.Genres {
			if gen.Name != "" {
				r.Genres = append(r.Genres, gen.Name)
			}
		}
		for _, p := range g.Platforms {
			if p.Name != "" {
				r.Platforms = append(r.Platforms, p.Name)
			}
		}
		for _, ic := range g.InvolvedCompanies {
			if ic.Company == nil {
				continue
			}
			if ic.Developer && r.Developer == "" {
				r.Developer = ic.Company.Name
			}
			if ic.Publisher && r.Publisher == "" {
				r.Publisher = ic.Company.Name
			}
		}
		results = append(results, r)
	}
	return results, nil
}

// igdbPlatform is the raw IGDB platform response.
type igdbPlatform struct {
	ID             int64      `json:"id"`
	Name           string     `json:"name"`
	PlatformLogo   *igdbCover `json:"platform_logo"`
}

// SearchPlatform queries IGDB /platforms for a console name and returns its logo URL.
func (c *Client) SearchPlatform(name string) (string, error) {
	safe := strings.ReplaceAll(name, `"`, `\"`)
	query := fmt.Sprintf(`fields id,name,platform_logo.url; search "%s"; limit 1;`, safe)

	token, err := c.getToken()
	if err != nil {
		return "", err
	}

	req, err := http.NewRequest(http.MethodPost, igdbPlatformsURL, bytes.NewBufferString(query))
	if err != nil {
		return "", err
	}
	req.Header.Set("Client-ID", c.clientID)
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Accept", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized {
		c.mu.Lock()
		c.cachedToken = ""
		c.mu.Unlock()
		return c.SearchPlatform(name)
	}
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("igdb platforms: HTTP %d", resp.StatusCode)
	}

	var platforms []igdbPlatform
	if err := json.NewDecoder(resp.Body).Decode(&platforms); err != nil {
		return "", err
	}

	if len(platforms) == 0 || platforms[0].PlatformLogo == nil {
		return "", nil
	}

	logoURL := strings.Replace(platforms[0].PlatformLogo.URL, "t_thumb", "t_logo_med", 1)
	if strings.HasPrefix(logoURL, "//") {
		logoURL = "https:" + logoURL
	}
	return logoURL, nil
}
