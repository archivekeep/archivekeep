package verification

import (
	"context"
	"fmt"
	"sort"

	"github.com/kr/pretty"
)

type VerifiableArchive interface {
	StoredFiles() ([]string, error)
	VerifyFileIntegrity(path string) error
}

func Verify(ctx context.Context, cmd pretty.Printfer, currentArchive VerifiableArchive) error {
	corruptedFiles := 0

	storedFiles, err := currentArchive.StoredFiles()
	if err != nil {
		return fmt.Errorf("retrieve current archive stored files: %w", err)
	}
	sort.Strings(storedFiles)

	cmd.Printf("Found %d files to verify\n", len(storedFiles))

	for n, path := range storedFiles {
		if err := currentArchive.VerifyFileIntegrity(path); err != nil {
			cmd.Printf("ERROR: verify %s: %v\n", path, err)
			corruptedFiles += 1
		}

		if (n+1)%10 == 0 || n+1 == len(storedFiles) {
			cmd.Printf("Verified %d of %d files\n", n+1, len(storedFiles))
		}

		select {
		case <-ctx.Done():
			return fmt.Errorf("interrupted")
		default:
		}
	}

	if corruptedFiles > 0 {
		return fmt.Errorf("%d corrupted files", corruptedFiles)
	}

	return nil
}
