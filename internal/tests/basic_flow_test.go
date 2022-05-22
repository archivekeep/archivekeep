package tests_test

import (
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestBasicFlow(t *testing.T) {
	_, archiveDir := testarchive.Create(t)

	createArchiveFile(t, archiveDir, "file_a", "A")

	out := runCmd(t, archiveDir, nil, "add", "file_a")
	assert.Equal(t, out, "added: file_a\n")

	out = runCmd(t, archiveDir, nil, "status")
	assert.Equal(t, out, "Total files in archive: 1\n")

	createArchiveFile(t, archiveDir, "file_b", "A")

	out = runCmd(t, archiveDir, nil, "status")
	assert.Equal(t, out, "\nFiles not added to the archive:\n\tfile_b\n\nTotal files in archive: 1\n")

	out = runCmd(t, archiveDir, nil, "add", ".")
	assert.Equal(t, out, "added: file_b\n")

	out = runCmd(t, archiveDir, nil, "status")
	assert.Equal(t, out, "Total files in archive: 2\n")

	out = runCmd(t, archiveDir, nil, "rm", "file_a")
	assert.Equal(t, out, "removed: file_a\n")

	out = runCmd(t, archiveDir, nil, "status")
	assert.Equal(t, out, "\nFiles not added to the archive:\n\tfile_a\n\nTotal files in archive: 1\n")
}
