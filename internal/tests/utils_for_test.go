package tests_test

import (
	"bytes"
	"fmt"
	"io"
	"os"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
	"github.com/archivekeep/archivekeep/internal/cli/cmd"
)

func generateTestContents(path, variant string) []byte {
	return []byte(fmt.Sprintf("%s: %s", path, variant))
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

func createMissingFiles(t *testing.T, archiveDir string, a *filesarchive.Archive, contents map[string]string) {
	t.Helper()

	if contents == nil {
		return
	}

	createArchiveFiles(t, archiveDir, contents)

	for filename := range contents {
		err := a.Add(filename)
		assert.NilError(t, err)
	}

	for filename := range contents {
		err := os.Remove(paths.Join(archiveDir, filename))
		assert.NilError(t, err)
	}
}

func createArchiveFile(t *testing.T, archiveDir, path, variant string) {
	t.Helper()

	err := os.MkdirAll(paths.Dir(paths.Join(archiveDir, path)), 0755)
	assert.NilError(t, err)

	err = os.WriteFile(paths.Join(archiveDir, path), generateTestContents(path, variant), 0755)
	assert.NilError(t, err)
}

func assertArchiveFile(t *testing.T, archiveDir, path, variant string) {
	t.Helper()

	contents, err := os.ReadFile(paths.Join(archiveDir, path))
	assert.NilError(t, err)
	assert.DeepEqual(t, contents, generateTestContents(path, variant))
}

func assertArchiveFileContains(t *testing.T, archiveDir, path, expectedContents string) {
	t.Helper()

	contents, err := os.ReadFile(paths.Join(archiveDir, path))
	assert.NilError(t, err)
	assert.DeepEqual(t, contents, []byte(expectedContents))
}

func runCmd(t *testing.T, dir string, stdin io.Reader, args ...string) string {
	t.Helper()

	out, err := runCmdBase(dir, stdin, args)
	assert.NilError(t, err)

	return out
}

func runCmdError(t *testing.T, dir string, stdin io.Reader, args ...string) (string, error) {
	t.Helper()

	out, err := runCmdBase(dir, stdin, args)
	assert.Check(t, err != nil)

	return out, err
}

func runCmdBase(dir string, stdin io.Reader, args []string) (string, error) {
	var out bytes.Buffer

	currentDir, _ := os.Getwd()
	defer os.Chdir(currentDir)

	err := os.Chdir(dir)
	if err != nil {
		return "", fmt.Errorf("change CWD for command execution: %w", err)
	}

	c := cmd.New()
	c.SetIn(stdin)
	c.SetOut(&out)
	c.SetArgs(args)
	err = c.Execute()

	return out.String(), err
}
