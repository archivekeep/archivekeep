package operations

import (
	"github.com/archivekeep/archivekeep/archive"
)

type WorkingArchive interface {
	archive.Reader
	archive.Writer

	FindAllFiles(searchGlobs ...string) []string

	Contains(path string) bool
	FileChecksum(path string) (string, error)
	ComputeFileChecksum(path string) (string, error)

	VerifyFileExists(path string) (bool, error)

	StoredFiles() ([]string, error)
}
