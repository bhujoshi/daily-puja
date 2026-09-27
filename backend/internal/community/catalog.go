package community

import (
	_ "embed"
	"encoding/json"
	"strings"
)

//go:embed catalog.json
var catalogJSON []byte

type CatalogOption struct {
	ID string `json:"id"`
}
type CatalogCategory struct {
	ID      string          `json:"id"`
	Options []CatalogOption `json:"options"`
}

func validSelections(selections map[string]string) bool {
	var catalog struct {
		Categories []CatalogCategory `json:"categories"`
	}
	if json.Unmarshal(catalogJSON, &catalog) != nil || len(selections) != len(catalog.Categories) {
		return false
	}
	for _, category := range catalog.Categories {
		ids := []string{selections[category.ID]}
		if category.ID == "flowers" {
			ids = strings.Split(selections[category.ID], ",")
		}
		seen := map[string]bool{}
		for _, id := range ids {
			found := false
			for _, option := range category.Options {
				if id == option.ID {
					found = true
					break
				}
			}
			if !found || seen[id] {
				return false
			}
			seen[id] = true
		}
	}
	return true
}
