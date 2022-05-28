//go:build embed_assets

package spa

import (
	"bytes"
	"embed"
	"fmt"
	"io"
	"io/fs"
	"net/http"
	paths "path"
	"strings"
	"time"
)

//go:embed embedded-assets
var embeddedAssetsFS embed.FS

var assets fs.FS

func getAsset(path string) (fs.File, error) {
	unreturnedFile, err := embeddedAssetsFS.Open(paths.Join("embedded-assets", path))
	if err != nil {
		return nil, err
	}
	defer func() {
		if unreturnedFile != nil {
			unreturnedFile.Close()
		}
	}()

	stat, err := unreturnedFile.Stat()
	if err != nil {
		return nil, fmt.Errorf("stat: %w", err)
	}
	if stat.IsDir() {
		return nil, fmt.Errorf("is directory: %w", fs.ErrNotExist)
	}

	// consume unreturnedFile for return to not defer-close
	result := unreturnedFile
	unreturnedFile = nil

	return result, nil
}

func init() {
	var modTime = time.Now()

	HandlerFuncFactory = func(config Config) http.HandlerFunc {
		return createWebUIHandler(
			func(path string) (io.ReadSeeker, time.Time, error) {
				rs, err := getAsset(path)
				if err != nil {
					return nil, modTime, err
				}

				if strings.HasSuffix(path, ".js") || strings.HasSuffix(path, ".css") {
					patchedContents, err := patchConstants(rs, config)
					if err != nil {
						return nil, modTime, fmt.Errorf("patch resource file: %w", err)
					}
					return bytes.NewReader(patchedContents), modTime, nil
				}

				return rs.(io.ReadSeeker), modTime, err
			},
			func() (io.ReadSeeker, time.Time, error) {
				rs, err := getAsset("index.html")
				if err != nil {
					return nil, modTime, err
				}

				patchedContents, err := patchConstants(rs, config)
				if err != nil {
					return nil, modTime, fmt.Errorf("patch index file: %w", err)
				}

				return bytes.NewReader(patchedContents), modTime, nil
			},
		)
	}
}
