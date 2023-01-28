package operations_test

import (
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive/operations"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestAdd_Prepare(t *testing.T) {
	a := testarchive.CreateTestingArchive01(t)

	a.CreateUnindexedArchiveFiles(map[string]string{
		"new_c":         "file_c",
		"new-dir/d":     "file_d",
		"new-dir/new-e": "file_e",
		"dir/c":         "dir file c",
		"dir/f":         "dir file f",
		"other/d":       "other file d",
		"other/g":       "other file g",
	})

	a.CreateMissingFiles(map[string]string{
		"old-dir/c":     "file_c",
		"old-dir/d":     "file_d",
		"old-dir/e":     "file_e",
		"deleted-dir/x": "deleted file X",
		"deleted-dir/y": "deleted file Y",
	})

	preparedAdd, err := operations.Add{
		SearchGlobs:       []string{"."},
		DisableMovesCheck: false,
	}.Prepare(a, "")

	assert.NilError(t, err)
	assert.DeepEqual(t, operations.AddPrepareResult{
		NewFiles: []string{
			"dir/c",
			"dir/f",
			"other/d",
			"other/g",
		},

		Moves: []operations.Move{
			{From: "old-dir/c", To: "new_c"},
			{From: "old-dir/d", To: "new-dir/d"},
			{From: "old-dir/e", To: "new-dir/new-e"},
		},

		MissingFiles: []string{
			"deleted-dir/x",
			"deleted-dir/y",
		},
	}, preparedAdd)
}
