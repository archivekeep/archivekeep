package operations_test

import (
	"strings"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive/operations"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestAdd_Prepare(t *testing.T) {
	a := createTestArchiveForAdd(t)

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
			"photos/darktable_exported_final/final_03.jpg",
			"photos/saved_darktable_exported/good_one_32.jpg",
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

func TestAdd_Of_Invalid_characters(t *testing.T) {
	type testCase struct {
		filename string
		err      string
	}

	testCases := []testCase{
		{
			":colon-not-allowed:",
			"file :colon-not-allowed: has invalid filename: character ':' is not supported in FAT-like filesystems",
		},
		{
			"no * here",
			"file no * here has invalid filename: character '*' is not supported in FAT-like filesystems",
		},
		{
			"what ? is this",
			"file what ? is this has invalid filename: character '?' is not supported in FAT-like filesystems",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.filename, func(t *testing.T) {
			a := createTestArchiveForAdd(t)

			a.CreateUnindexedArchiveFiles(map[string]string{
				tc.filename: "",
			})

			_, err := operations.Add{
				SearchGlobs:       []string{"."},
				DisableMovesCheck: false,
			}.Prepare(a, "")

			assert.Error(t, err, tc.err)
		})
	}
}

func TestAdd_Prepare_WithDisabledMovesCheck(t *testing.T) {
	a := createTestArchiveForAdd(t)

	preparedAdd, err := operations.Add{
		SearchGlobs:       []string{"."},
		DisableMovesCheck: true,
	}.Prepare(a, "")

	assert.NilError(t, err)
	assert.DeepEqual(t, operations.AddPrepareResult{
		NewFiles: []string{
			"dir/c",
			"dir/f",
			"new-dir/d",
			"new-dir/new-e",
			"new_c",
			"other/d",
			"other/g",
			"photos/darktable_exported_final/final_03.jpg",
			"photos/saved_darktable_exported/good_one_32.jpg",
		},
	}, preparedAdd)
}

func createTestArchiveForAdd(t *testing.T) *testarchive.TestArchive {
	a := testarchive.CreateTestingArchive01(t)

	a.CreateUnindexedArchiveFiles(map[string]string{
		".archivekeepignore": strings.Join([]string{
			"# the XMP is part of exported JPEGs anyway",
			"*.xmp",
			"*.XMP",
			"# default darktable_exported files attempts",
			"darktable_exported",
		}, "\n"),

		"new_c":         "file_c",
		"new-dir/d":     "file_d",
		"new-dir/new-e": "file_e",
		"dir/c":         "dir file c",
		"dir/f":         "dir file f",
		"other/d":       "other file d",
		"other/g":       "other file g",
		"tralala.xmp":   "an XMP file",

		"photos/darktable_exported/attempt_01.jpg": "attempt 01.jpg",
		"photos/darktable_exported/attempt_02.jpg": "attempt 02.jpg",

		"photos/saved_darktable_exported/good_one_32.jpg": "A good one",
		"photos/darktable_exported_final/final_03.jpg":    "A final one",
	})

	a.CreateMissingFiles(map[string]string{
		"old-dir/c":     "file_c",
		"old-dir/d":     "file_d",
		"old-dir/e":     "file_e",
		"deleted-dir/x": "deleted file X",
		"deleted-dir/y": "deleted file Y",
	})
	return a
}
