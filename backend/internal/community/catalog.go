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

func catalogItem(categoryID, itemID string) bool {
	var catalog struct {
		Categories []CatalogCategory `json:"categories"`
	}
	if json.Unmarshal(catalogJSON, &catalog) != nil {
		return false
	}
	for _, category := range catalog.Categories {
		if category.ID == categoryID {
			for _, item := range category.Options {
				if item.ID == itemID {
					return true
				}
			}
		}
	}
	return false
}

func validSelections(selections map[string]string) bool {
	var catalog struct {
		Categories []CatalogCategory `json:"categories"`
	}
	if json.Unmarshal(catalogJSON, &catalog) != nil {
		return false
	}
	if _, ok := selections["garland"]; !ok && len(selections) == len(catalog.Categories)-1 {
		copied := map[string]string{"garland": "original"}
		for k, v := range selections {
			copied[k] = v
		}
		selections = copied
	}
	if len(selections) != len(catalog.Categories) {
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
