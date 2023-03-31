package filesarchive

import (
	"os"
	paths "path"
	"strings"
	"testing"

	"github.com/spf13/afero"
	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/x/testing/archivetest"
)

func TestInitialize(t *testing.T) {
	t.SkipNow()

	// TODO: test following scenarios:
	//  - initialize new archive, assert created directories
	//  - initialize existing archive, assert error
}

func TestOpen(t *testing.T) {
	t.SkipNow()

	// TODO: test following scenarios:
	//  - open existing archive
	//  - open non-archive existing directory
	//  - open non-existing directory
	//  - open non-root directory within existing archive
}

func TestArchive_MoveFile(t *testing.T) {
	t.SkipNow()

	// TODO: test following scenarios:
	//  - within same directory,
	//  - to other non-existing directory in archive,
	//  - to other existing directory in archive,
	//  - to existing file added to archive index (don't overwrite)
	//  - to existing file not-added to archive index (don't overwrite)
}

func TestArchive_SaveFile_WontModifyExistingNonIndexedFile(t *testing.T) {
	archiveDir, archive := createTestArchive01(t)

	err := archive.SaveFileFromBytes([]byte("new contents"), "non-indexed-file")
	assert.ErrorContains(t, err, "/non-indexed-file: file exists")

	assertArchiveFileContains(t, archiveDir, "indexed-file", "indexed file contents")
	assertArchiveFileContains(t, archiveDir, "non-indexed-file", "non indexed file contents")
}

func TestArchiveContract(t *testing.T) {
	archivetest.Run(t, createArchiveImplementationTester())
}

func createArchiveImplementationTester() *archivetest.ImplementationTester {
	return &archivetest.ImplementationTester{
		New: func(t *testing.T) *archivetest.TestedArchive {
			archiveDir := t.TempDir()
			_, err := Init(afero.NewOsFs(), archiveDir)
			assert.NilError(t, err)

			return &archivetest.TestedArchive{
				OpenReadWriter: func() archive.ReadWriter {
					arw, err := Open(afero.NewOsFs(), archiveDir)
					assert.NilError(t, err)

					return arw
				},
				StoreFile: func(path string, contents []byte) {
					// TODO: should skip implementation

					arw, err := Open(afero.NewOsFs(), archiveDir)
					assert.NilError(t, err)

					err = arw.SaveFileFromBytes(contents, path)
					assert.NilError(t, err)
				},
				ReadFileContents: func(path string) []byte {
					contents, err := os.ReadFile(paths.Join(archiveDir, path))
					assert.NilError(t, err)

					return contents
				},
			}
		},

		// TODO: add capability to modify tested archive to check non-indexed
	}
}

func createTestArchive01(t *testing.T) (string, *Archive) {
	archiveDir, archive := createTempArchiveWithContents(t, map[string]string{
		"indexed-file": "indexed file contents",
	})
	createArchiveFiles(t, archiveDir, map[string]string{
		"non-indexed-file": "non indexed file contents",
	})
	return archiveDir, archive
}

func createTempArchiveWithContents(t *testing.T, contents map[string]string) (string, *Archive) {
	t.Helper()

	archiveDir := t.TempDir()
	archive, err := Init(afero.NewOsFs(), archiveDir)
	assert.NilError(t, err)

	createArchiveFiles(t, archiveDir, contents)

	for filename := range contents {
		err := archive.Add(filename)
		assert.NilError(t, err)
	}

	return archiveDir, archive
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

func assertArchiveFileContains(t *testing.T, archiveDir, path, expectedContents string) {
	t.Helper()

	contents, err := os.ReadFile(paths.Join(archiveDir, path))
	assert.NilError(t, err)
	assert.DeepEqual(t, contents, []byte(expectedContents))
}

func TestArchive_FindAllFiles(t *testing.T) {
	t.Run("Ignores files by patterns from .archivekeepignore", func(t *testing.T) {
		aDir, a := createTestArchive01(t)

		createArchiveFiles(t, aDir, map[string]string{
			".archivekeepignore": strings.Join([]string{
				"# the XMP is part of exported JPEGs anyway",
				"*.xmp",
				"*.XMP",
			}, "\n"),

			"photos/20010203.RW2":          "RAW file",
			"photos/20010203_edit_01.JPEG": "JPEG of edit 01 with XMP profile attached",
			"photos/20010203_edit_01.XMP":  "XMP of edit 01",
		})

		foundFiles, err := a.FindAllFiles(".")
		assert.NilError(t, err)
		assert.DeepEqual(t, []string{
			"indexed-file",
			"non-indexed-file",
			"photos/20010203.RW2",
			"photos/20010203_edit_01.JPEG",
		}, foundFiles)
	})
}
