//go:build embed_version

package buildinfo

import (
	_ "embed"
	"encoding/json"
	"fmt"
)

//go:embed archivekeep-version.json
var VersionJSON string

func GetVersion() (Version, error) {
	var version Version

	err := json.Unmarshal([]byte(VersionJSON), &version)
	if err != nil {
		return Version{}, fmt.Errorf("parse VersionJSON: %w", err)
	}

	return version, nil
}
