package tests_test

import (
	"os"
	"testing"

	"gotest.tools/v3/assert"
)

func TestInitArchive(t *testing.T) {
	archiveDir := t.TempDir()

	err := os.MkdirAll(archiveDir+"/existing-sub-dir", 0644)
	assert.NilError(t, err)

	out := runCmd(t, archiveDir, nil, "init")
	assert.Equal(t, out, terminalLines(
		"Archive successfully initialized",
	))
}

func TestInitArchiveAgainFails(t *testing.T) {
	archiveDir := t.TempDir()

	runCmd(t, archiveDir, nil, "init")

	_, err := runCmdError(t, archiveDir, nil, "init")
	assert.ErrorContains(t, err, "initialization failed: archive already exists")
}

func TestInitReadOnlyDirFails(t *testing.T) {
	archiveDir := t.TempDir()
	assert.NilError(t, os.Chmod(archiveDir, 0500))

	_, err := runCmdError(t, archiveDir, nil, "init")
	assert.ErrorContains(t, err, "initialization failed")
	assert.ErrorContains(t, err, "mkdir .archive: permission denied")
}
