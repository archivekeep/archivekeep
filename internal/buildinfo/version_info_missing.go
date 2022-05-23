//go:build !embed_version

package buildinfo

import (
	_ "embed"
	"fmt"
)

var VersionJSON string = "version information not present"

func GetVersion() (Version, error) {
	return Version{}, fmt.Errorf("version information not present")
}
