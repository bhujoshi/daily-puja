package community

import (
	_ "embed"
	"encoding/json"
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
		found := false
		for _, option := range category.Options {
			if selections[category.ID] == option.ID {
				found = true
				break
			}
		}
		if !found {
			return false
		}
	}
	return true
}
