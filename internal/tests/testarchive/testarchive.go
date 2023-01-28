package testarchive

import (
	"os"
	paths "path"
	"testing"

	"github.com/spf13/afero"
	"gotest.tools/v3/assert"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
)

type TestArchive struct {
	t testing.TB

	Dir string

	*filesarchive.Archive
}

func Create(t *testing.T) *TestArchive {
	t.Helper()

	dir := t.TempDir()
	a, err := filesarchive.Init(afero.NewOsFs(), dir)
	assert.NilError(t, err)

	return &TestArchive{
		t: t,

		Dir: dir,

		Archive: a,
	}
}

func CreateWithContents(t *testing.T, contents map[string]string) *TestArchive {
	t.Helper()

	a := Create(t)
	a.CreateUnindexedArchiveFiles(contents)

	for filename := range contents {
		err := a.Add(filename)
		assert.NilError(t, err)
	}

	return a
}

func CreateTestingArchive01(t *testing.T) *TestArchive {
	return CreateWithContents(t, map[string]string{
		"a":       "file_a",
		"b":       "file_b",
		"dir/a":   "dir file a",
		"dir/b":   "dir file b",
		"other/a": "other file a",
		"other/c": "other file c",
	})
}

func (ta *TestArchive) CreateUnindexedArchiveFiles(contents map[string]string) {
	ta.t.Helper()

	if contents == nil {
		return
	}

	for filename, fileContent := range contents {
		err := os.MkdirAll(paths.Dir(paths.Join(ta.Dir, filename)), 0755)
		assert.NilError(ta.t, err)

		err = os.WriteFile(paths.Join(ta.Dir, filename), []byte(fileContent), 0755)
		assert.NilError(ta.t, err)
	}
}

func (ta *TestArchive) CreateMissingFiles(contents map[string]string) {
	ta.t.Helper()

	if contents == nil {
		return
	}

	ta.CreateUnindexedArchiveFiles(contents)

	for filename := range contents {
		err := ta.Add(filename)
		assert.NilError(ta.t, err)
	}

	for filename := range contents {
		err := os.Remove(paths.Join(ta.Dir, filename))
		assert.NilError(ta.t, err)
	}
}
