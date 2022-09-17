package assertarchive

import (
	"sort"
	"testing"

	"github.com/spf13/afero"
	"gotest.tools/v3/assert"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
)

func IndexContains(t testing.TB, archiveDir string, expectedFiles []string) {
	t.Helper()

	a, err := filesarchive.Open(afero.NewOsFs(), archiveDir)
	assert.NilError(t, err)

	storedFiles, err := a.StoredFiles()
	assert.NilError(t, err)

	sortedStoredFiles := sortCopy(storedFiles)
	expectedFiles = sortCopy(expectedFiles)

	assert.DeepEqual(t, sortedStoredFiles, expectedFiles)
}

func sortCopy(s []string) []string {
	s = append([]string{}, s...)
	sort.Strings(s)
	return s
}
