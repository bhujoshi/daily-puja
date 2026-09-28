// Package music serves the curated catalog without provider searches at request time.
package music

import (
	"crypto/sha256"
	_ "embed"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
)

//go:embed catalog.json
var bundled []byte

type Track struct {
	ID         string `json:"id"`
	Order      int    `json:"order"`
	Title      string `json:"title"`
	Collection string `json:"collection"`
	Deity      string `json:"deity"`
	Thumbnail  string `json:"thumbnail"`
	AudioURL   string `json:"audioUrl"`
	Artist     string `json:"artist"`
	SourceURL  string `json:"sourceUrl"`
}
type Catalog struct {
	Tracks []Track `json:"tracks"`
}

// New loads metadata once. MUSIC_CATALOG_PATH can supply verified recordings at deployment.
func New(path string) (http.Handler, error) {
	data := bundled
	if path != "" {
		var err error
		data, err = os.ReadFile(path)
		if err != nil {
			return nil, err
		}
	}
	var catalog Catalog
	if err := json.Unmarshal(data, &catalog); err != nil {
		return nil, err
	}
	seen := map[string]bool{}
	for _, t := range catalog.Tracks {
		if t.ID == "" || seen[t.ID] || t.Title == "" || t.Deity == "" {
			return nil, fmt.Errorf("invalid or duplicate music track %q", t.ID)
		}
		seen[t.ID] = true
		if t.AudioURL != "" {
			u, err := url.Parse(t.AudioURL)
			if err != nil || u.Scheme != "https" || u.Host == "" {
				return nil, fmt.Errorf("track %s requires HTTPS audio", t.ID)
			}
		}
	}
	data, _ = json.Marshal(catalog)
	etag := fmt.Sprintf(`"%x"`, sha256.Sum256(data))
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet && r.Method != http.MethodHead {
			w.Header().Set("Allow", "GET, HEAD")
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Header().Set("Cache-Control", "public, max-age=3600")
		w.Header().Set("ETag", etag)
		if r.Header.Get("If-None-Match") == etag {
			w.WriteHeader(http.StatusNotModified)
			return
		}
		if r.Method == http.MethodGet {
			_, _ = w.Write(data)
		}
	}), nil
}
