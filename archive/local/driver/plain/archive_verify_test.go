package filesarchive

import (
	"context"
	"fmt"
	"strings"
	"testing"

	"github.com/spf13/afero"
	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/x/operations/verification"
)

func TestArchive_Verify(t *testing.T) {
	t.Run("Valid archive", func(t *testing.T) {
		var log sbuf
		a, _ := createValidArchive(t)

		_, err := verification.Execute(context.Background(), a, verification.ActionOptions{
			Logger: &log,
		})
		assert.NilError(t, err)
		assert.DeepEqual(t, strings.Join(log, ""), lines(
			"Found 4 files to verify",
			"Verified 4 of 4 files",
			"Verification completed",
			"OK files:        4",
			"Corrupted files: 0",
		))
	})
	t.Run("Continue", func(t *testing.T) {
		a, _ := createValidArchive(t)

		var log01 sbuf
		_, err := verification.Execute(context.Background(), a, verification.ActionOptions{
			Logger:           &log01,
			LogVerifiedFiles: true,
		})
		assert.NilError(t, err)
		assert.DeepEqual(t, lines(
			"Found 4 files to verify",
			"INFO: valid file file_a",
			"INFO: valid file file_b",
			"INFO: valid file file_c",
			"INFO: valid file file_d",
			"Verified 4 of 4 files",
			"Verification completed",
			"OK files:        4",
			"Corrupted files: 0",
		), strings.Join(log01, ""))

		createArchiveFile(t, a, "file_x", "22")
		createArchiveFile(t, a, "file_y", "23")
		createArchiveFile(t, a, "file_z", "24")

		var log02 sbuf
		_, err = verification.Execute(context.Background(), a, verification.ActionOptions{
			Logger:           &log02,
			LogVerifiedFiles: true,
		})
		assert.NilError(t, err)
		assert.DeepEqual(t, lines(
			"Found 3 files to verify",
			"INFO: valid file file_x",
			"INFO: valid file file_y",
			"INFO: valid file file_z",
			"Verified 3 of 3 files",
			"Verification completed",
			"OK files:        3",
			"Corrupted files: 0",
		), strings.Join(log02, ""))
	})
}

func createValidArchive(t *testing.T) (*Archive, string) {
	a, archiveDir := Create(t)

	createArchiveFile(t, a, "file_a", "01")
	createArchiveFile(t, a, "file_b", "02")
	createArchiveFile(t, a, "file_c", "03")
	createArchiveFile(t, a, "file_d", "04")

	return a, archiveDir
}

func Create(t *testing.T) (*Archive, string) {
	t.Helper()

	dir := t.TempDir()
	a, err := Init(afero.NewOsFs(), dir)
	assert.NilError(t, err)

	return a, dir
}

func createArchiveFile(t *testing.T, a *Archive, path, variant string) {
	t.Helper()

	err := a.SaveFileFromBytes(generateTestContents(path, variant), path)
	assert.NilError(t, err)
}

func generateTestContents(path, variant string) []byte {
	return []byte(fmt.Sprintf("%s: %s", path, variant))
}

type sbuf []string

func (p *sbuf) Printf(format string, a ...interface{}) {
	s := fmt.Sprintf(format, a...)
	*p = append(*p, s)
}

func lines(lines ...string) string {
	return strings.Join(lines, "\n") + "\n"
}
