package testarchive

import (
	"os"
	paths "path"
	"testing"

	"github.com/spf13/afero"
	"gotest.tools/v3/assert"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
)

func Create(t *testing.T) (*filesarchive.Archive, string) {
	t.Helper()

	dir := t.TempDir()
	a, err := filesarchive.Init(afero.NewOsFs(), dir)
	assert.NilError(t, err)

	return a, dir
}

func CreateWithContents(t *testing.T, contents map[string]string) (*filesarchive.Archive, string) {
	t.Helper()

	a, archiveDir := Create(t)
	createArchiveFiles(t, archiveDir, contents)

	for filename := range contents {
		err := a.Add(filename)
		assert.NilError(t, err)
	}

	return a, archiveDir
}

func CreateTestingArchive01(t *testing.T) (*filesarchive.Archive, string) {
	return CreateWithContents(t, map[string]string{
		"a":       "file_a",
		"b":       "file_b",
		"dir/a":   "dir file a",
		"dir/b":   "dir file b",
		"other/a": "other file a",
		"other/c": "other file c",
	})
}

func createArchiveFiles(t *testing.T, archiveDir string, contents map[string]string) {
	t.Helper()

	if contents == nil {
		return
	}

	for filename, fileContent := range contents {
		err := os.MkdirAll(paths.Dir(paths.Join(archiveDir, filename)), 0755)
		assert.NilError(t, err)

		err = os.WriteFile(paths.Join(archiveDir, filename), []byte(fileContent), 0755)
		assert.NilError(t, err)
	}
}
