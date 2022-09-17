package cryptoarchive

import (
	"bytes"
	"os"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
	akcf "github.com/archivekeep/archivekeep/crypto/file"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/x/testing/archivetest"
)

func TestCreate(t *testing.T) {
	archiveDir := t.TempDir()

	_, err := Create(
		archiveDir,
		constantPassword(archiveDir),
	)
	assert.NilError(t, err)

	assert.Assert(t, util.FileExists(paths.Join(archiveDir, "ArchiveKeep", "encryption", "keyring")))
}

func TestArchive_FileChecksum(t *testing.T) {
	t.SkipNow()

	// TODO: test checksum of a file
}

func TestArchive_Location(t *testing.T) {
	archiveDir, archive := createTestArchive01(t)

	assert.DeepEqual(t, archive.Location(), archiveDir)
}

func TestArchiveContract(t *testing.T) {
	archivetest.Run(t, createImplementationTester())
}

func createImplementationTester() *archivetest.ImplementationTester {
	return &archivetest.ImplementationTester{
		New: func(t *testing.T) *archivetest.TestedArchive {
			archiveDir := t.TempDir()
			a, err := Create(archiveDir, constantPassword(archiveDir))
			assert.NilError(t, err)

			return &archivetest.TestedArchive{
				OpenReadWriter: func() archive.ReadWriter {
					arw, err := Open(archiveDir, constantPassword(archiveDir))
					assert.NilError(t, err)
					return arw
				},
				StoreFile: func(path string, contents []byte) {
					// TODO: should skip implementation

					arw, err := Open(archiveDir, constantPassword(archiveDir))
					assert.NilError(t, err)

					err = arw.SaveFileFromBytes(contents, path)
					assert.NilError(t, err)
				},
				ReadFileContents: func(path string) []byte {
					encryptedContents, err := os.ReadFile(paths.Join(archiveDir, "EncryptedFiles", path+".enc"))
					assert.NilError(t, err)

					_, data, err := akcf.Decrypt(bytes.NewReader(encryptedContents), &akcf.DecryptOptions{
						Keyring:    a.keyring,
						SigKeyring: a.keyring,
					})
					assert.NilError(t, err)
					return data
				},
			}
		},
	}
}

func createTestArchive01(t *testing.T) (string, *Archive) {
	archiveDir, archive := createTempArchiveWithContents(t, map[string]string{
		"existing-file-01": "existing file contents",
		"existing-file-02": "existing another file contents",
	})
	return archiveDir, archive
}

func createTempArchiveWithContents(t testing.TB, contents map[string]string) (string, *Archive) {
	t.Helper()

	archiveDir := t.TempDir()
	archive, err := Create(archiveDir, constantPassword(archiveDir))
	assert.NilError(t, err)

	for filename, value := range contents {
		err := archive.SaveFileFromBytes([]byte(value), filename)
		assert.NilError(t, err)
	}

	archive, err = Open(archiveDir, constantPassword(archiveDir))
	assert.NilError(t, err)

	return archiveDir, archive
}

func constantPassword(s string) func() (string, error) {
	return func() (string, error) {
		return s, nil
	}
}
