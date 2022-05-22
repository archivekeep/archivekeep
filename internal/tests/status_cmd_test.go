package tests_test

import (
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestStatusFromSubDirectory(t *testing.T) {
	_, archiveDir := testarchive.CreateTestingArchive01(t)

	// test initial archive
	{
		var out string

		out = runCmd(t, archiveDir, nil, "status")
		assert.Equal(t, out, "Total files in archive: 6\n")

		out = runCmd(t, paths.Join(archiveDir, "dir"), nil, "status")
		assert.Equal(t, out, "Total files in archive: 6\n")

		out = runCmd(t, paths.Join(archiveDir, "other"), nil, "status")
		assert.Equal(t, out, "Total files in archive: 6\n")
	}

	createArchiveFiles(t, archiveDir, map[string]string{
		"c":       "file_c",
		"d":       "file_d",
		"e":       "file_e",
		"dir/c":   "dir file c",
		"dir/f":   "dir file f",
		"other/d": "other file d",
		"other/g": "other file g",
	})

	// test archive with non-added files
	{
		var out string

		out = runCmd(t, archiveDir, nil, "status")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\tc\n\td\n\tdir/c\n\tdir/f\n\te\n\tother/d\n\tother/g\n\nTotal files in archive: 6\n")

		out = runCmd(t, paths.Join(archiveDir, "dir"), nil, "status")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\t../c\n\t../d\n\tc\n\tf\n\t../e\n\t../other/d\n\t../other/g\n\nTotal files in archive: 6\n")

		out = runCmd(t, paths.Join(archiveDir, "other"), nil, "status")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\t../c\n\t../d\n\t../dir/c\n\t../dir/f\n\t../e\n\td\n\tg\n\nTotal files in archive: 6\n")

		out = runCmd(t, archiveDir, nil, "status", "dir")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\tdir/c\n\tdir/f\n\nFiles in archive matching globs: 2\n")

		out = runCmd(t, paths.Join(archiveDir, "dir"), nil, "status", ".")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\tc\n\tf\n\nFiles in archive matching globs: 2\n")

		out = runCmd(t, paths.Join(archiveDir, "dir"), nil, "status", "../other")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\t../other/d\n\t../other/g\n\nFiles in archive matching globs: 2\n")

		out = runCmd(t, paths.Join(archiveDir, "dir"), nil, "status", ".", "../other")
		assert.Equal(t, out, "\nFiles not added to the archive:\n\tc\n\tf\n\t../other/d\n\t../other/g\n\nFiles in archive matching globs: 4\n")
	}
}
