package tests_test

import (
	"os"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestVerify(t *testing.T) {
	_, archiveDir := testarchive.Create(t)

	createArchiveFile(t, archiveDir, "file_a", "A")
	createArchiveFile(t, archiveDir, "file_b", "A")
	createArchiveFile(t, archiveDir, "file_c", "A")
	createArchiveFile(t, archiveDir, "file_d", "A")

	out := runCmd(t, archiveDir, nil, "add", "file_*")
	assert.Equal(t, out, "added: file_a\nadded: file_b\nadded: file_c\nadded: file_d\n")

	out = runCmd(t, archiveDir, nil, "verify")
	assert.Equal(t, out, "Found 4 files to verify\nVerified 4 of 4 files\n")

	createArchiveFile(t, archiveDir, "file_b", "C")
	os.Remove(paths.Join(archiveDir, "file_d"))

	out, _ = runCmdError(t, archiveDir, nil, "verify")
	assert.Equal(t, out, "Found 4 files to verify\nERROR: verify file_b: file was modified\nERROR: verify file_d: file was deleted\nVerified 4 of 4 files\n")
}
