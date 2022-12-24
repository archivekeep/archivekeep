package tests_test

import (
	"os"
	paths "path"
	"strings"
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

	out := runCmd(t, archiveDir, nil, "add", "--do-not-print-preparation-summary", "file_*")
	assert.Equal(t, lines(
		"added: file_a",
		"added: file_b",
		"added: file_c",
		"added: file_d",
	), out)

	out = runCmd(t, archiveDir, nil, "verify")
	assert.Equal(t, lines(
		"Found 4 files to verify",
		"Verified 4 of 4 files",
		"Verification completed",
		"OK files:        4",
		"Corrupted files: 0",
	), out)

	createArchiveFile(t, archiveDir, "file_b", "C")
	os.Remove(paths.Join(archiveDir, "file_d"))

	out, _ = runCmdError(t, archiveDir, nil, "verify", "--again")
	assert.Equal(t, lines(
		"Found 4 files to verify",
		"ERROR: corrupted file_b: file was modified",
		"ERROR: corrupted file_d: file was deleted",
		"Verified 4 of 4 files",
		"Verification completed",
		"OK files:        2",
		"Corrupted files: 2",
	), out)
}

func lines(lines ...string) string {
	return strings.Join(lines, "\n") + "\n"
}
