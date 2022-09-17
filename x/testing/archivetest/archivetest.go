package archivetest

import (
	"bytes"
	"io"
	"io/ioutil"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
)

type TestedArchive struct {
	OpenReadWriter func() archive.ReadWriter

	// Store file directly and skip archive driver implementation
	// in order to test correctness of archive implementation
	StoreFile func(path string, contents []byte)

	// Read contents of file directly and skip archive implementation
	// in order to test correctness of archive implementation
	ReadFileContents func(path string) []byte
}

type ImplementationTester struct {
	New func(t *testing.T) *TestedArchive
}

func Run(t *testing.T, implementation *ImplementationTester) {
	t.Run("OpenFile", func(t *testing.T) {
		testedArchive := createTestArchive01(t, implementation)
		archiveRW := testedArchive.OpenReadWriter()

		info, contentsReader, err := archiveRW.OpenFile("existing-file-01")
		assert.NilError(t, err)
		defer contentsReader.Close()

		assert.DeepEqual(t, info, archive.FileInfo{
			Path:   "existing-file-01",
			Length: 22,
			Digest: map[string]string{
				"SHA256": "2a5c7f36d8af12221b84b588566aa964668fb7fc0f5d717f906a25bbab8798d4",
			},
		})

		contents, err := ioutil.ReadAll(contentsReader)
		assert.NilError(t, err)

		assert.DeepEqual(t, string(contents), "existing file contents")
	})

	t.Run("OpenSeekableFile", func(t *testing.T) {
		testedArchive := createTestArchive01(t, implementation)
		archiveRW := testedArchive.OpenReadWriter()

		info, contentsReader, err := archiveRW.OpenSeekableFile("existing-file-01")
		assert.NilError(t, err)
		defer contentsReader.Close()

		assert.DeepEqual(t, info, *existingFile01Info)

		expectedData := []byte("existing file contents")

		for i := len(expectedData) - 1; i >= 0; i-- {
			_, err := contentsReader.Seek(int64(i), io.SeekStart)
			assert.NilError(t, err)

			var b [1]byte
			n, err := contentsReader.Read(b[:])
			assert.NilError(t, err, "read: %d", i)
			assert.DeepEqual(t, n, 1)
			assert.DeepEqual(t, expectedData[i], b[0])
		}
	})

	t.Run("OpenReaderAtFile", func(t *testing.T) {
		testedArchive := createTestArchive01(t, implementation)
		archiveRW := testedArchive.OpenReadWriter()

		info, contentsReader, err := archiveRW.OpenReaderAtFile("existing-file-01")
		assert.NilError(t, err)
		defer contentsReader.Close()

		assert.DeepEqual(t, info, *existingFile01Info)

		expectedData := []byte("existing file contents")
		b := make([]byte, len(expectedData)+100)

		for offset := len(expectedData) - 1; offset >= 0; offset-- {
			for limit := 1; offset+limit < len(expectedData); limit++ {
				n, err := contentsReader.ReadAt(b[0:limit], int64(offset))
				assert.NilError(t, err)
				assert.DeepEqual(t, n, limit)
				assert.DeepEqual(t, expectedData[offset:offset+limit], b[0:limit])
			}
		}

		for offset := 0; offset <= len(expectedData); offset++ {
			expectedReadSize := len(expectedData) - offset

			n, err := contentsReader.ReadAt(b[:], int64(offset))
			if err != nil {
				assert.ErrorContains(t, err, "EOF")
			}
			assert.DeepEqual(t, n, expectedReadSize)
			assert.DeepEqual(t, expectedData[offset:offset+expectedReadSize], b[0:expectedReadSize])
		}
	})

	t.Run("SaveFile", func(t *testing.T) {
		t.Run("Saves new file", func(t *testing.T) {
			testedArchive := createTestArchive01(t, implementation)
			archiveRW := testedArchive.OpenReadWriter()

			err := archiveRW.SaveFile(
				bytes.NewReader(newFileContent),
				&archive.FileInfo{
					Path:   "new-file",
					Length: 17,
					Digest: map[string]string{
						"SHA256": newFileSha256,
					},
				},
			)
			assert.NilError(t, err)

			assertArchiveFileContains(t, testedArchive, "existing-file-01", "existing file contents")
			assertArchiveFileContains(t, testedArchive, "new-file", "new file contents")
			assertArchiveIndex(t, testedArchive, []*archive.FileInfo{
				existingFile01Info,
				existingFile02Info,
				newFileInfo,
			})
		})
		t.Run("Won't modify existing file", func(t *testing.T) {
			testedArchive := createTestArchive01(t, implementation)
			archiveRW := testedArchive.OpenReadWriter()

			err := archiveRW.SaveFile(
				bytes.NewReader(newFileContent),
				&archive.FileInfo{
					Path:   "existing-file-01",
					Length: 17,
					Digest: map[string]string{
						"SHA256": "428189279ce0f27c1e26d0555538043bae351115e3795b1d3ffcb2948de131dd",
					},
				},
			)
			assert.ErrorContains(t, err, "existing-file-01")
			assert.ErrorContains(t, err, "file exists")

			assertArchiveFileContains(t, testedArchive, "existing-file-01", "existing file contents")
			assertArchiveIndex(t, testedArchive, []*archive.FileInfo{
				existingFile01Info,
				existingFile02Info,
			})
		})
		t.Run("Won't store corrupted file and won't break correct save afterwards", func(t *testing.T) {
			testedArchive := createTestArchive01(t, implementation)
			archiveRW := testedArchive.OpenReadWriter()

			err := archiveRW.SaveFile(
				bytes.NewReader(newFileContent[0:8]),
				&archive.FileInfo{
					Path:   "new-file",
					Length: 8,
					Digest: map[string]string{
						"SHA256": newFileSha256,
					},
				},
			)
			assert.ErrorContains(t, err, "wrong checksum:")
			assert.ErrorContains(t, err, "got=b37d2cbfd875891e9ed073fcbe61f35a990bee8eecbdd07f9efc51339d5ffd66")
			assert.ErrorContains(t, err, "expected="+newFileSha256)
			assertArchiveIndex(t, testedArchive, []*archive.FileInfo{
				existingFile01Info,
				existingFile02Info,
			})

			err = archiveRW.SaveFile(
				bytes.NewReader(newFileContent),
				&archive.FileInfo{
					Path:   "new-file",
					Length: 17,
					Digest: map[string]string{
						"SHA256": newFileSha256,
					},
				},
			)
			assert.NilError(t, err)

			assertArchiveFileContains(t, testedArchive, "existing-file-01", "existing file contents")
			assertArchiveFileContains(t, testedArchive, "new-file", "new file contents")
			assertArchiveIndex(t, testedArchive, []*archive.FileInfo{
				existingFile01Info,
				existingFile02Info,
				newFileInfo,
			})
		})
		// TODO: test store large file (> 10MB)
	})

	t.Run("MoveFile", func(t *testing.T) {
		t.Run("Move file to new location", func(t *testing.T) {
			testedArchive := createTestArchive01(t, implementation)
			archiveRW := testedArchive.OpenReadWriter()

			err := archiveRW.MoveFile("existing-file-01", "new-file-location")
			assert.NilError(t, err)

			assertArchiveFileContains(t, testedArchive, "new-file-location", "existing file contents")
			assertArchiveFileContains(t, testedArchive, "existing-file-02", "existing another file contents")
		})

		t.Run("Try to overwrite another file", func(t *testing.T) {
			testedArchive := createTestArchive01(t, implementation)
			archiveRW := testedArchive.OpenReadWriter()

			err := archiveRW.MoveFile("existing-file-01", "existing-file-02")
			assert.ErrorContains(t, err, "destination file already exists: existing-file-02")

			assertArchiveFileContains(t, testedArchive, "existing-file-01", "existing file contents")
			assertArchiveFileContains(t, testedArchive, "existing-file-02", "existing another file contents")
		})
	})

	t.Run("ListFiles", func(t *testing.T) {
		testedArchive := createTestArchive01(t, implementation)
		archiveRW := testedArchive.OpenReadWriter()

		index, err := archiveRW.ListFiles()
		assert.NilError(t, err)
		assert.DeepEqual(t, index, []*archive.FileInfo{
			{
				Path:   "existing-file-01",
				Length: 22,
				Digest: map[string]string{
					"SHA256": "2a5c7f36d8af12221b84b588566aa964668fb7fc0f5d717f906a25bbab8798d4",
				},
			},
			{
				Path:   "existing-file-02",
				Length: 30,
				Digest: map[string]string{
					"SHA256": "e1bd0afc0d2276fab4d1458b12e01d760e0d0e2a7cf6f72fde281f297f8bff65",
				},
			},
		})
	})
}

func createTestArchive01(t *testing.T, implementation *ImplementationTester) *TestedArchive {
	return createTempArchiveWithContents(t, implementation, map[string]string{
		"existing-file-01": "existing file contents",
		"existing-file-02": "existing another file contents",
	})
}

func createTempArchiveWithContents(t *testing.T, implementation *ImplementationTester, contents map[string]string) *TestedArchive {
	t.Helper()

	testedArchive := implementation.New(t)

	for filename, value := range contents {
		testedArchive.StoreFile(filename, []byte(value))
	}

	return testedArchive
}

func assertArchiveFileContains(t *testing.T, testedArchive *TestedArchive, path, expectedContents string) {
	t.Helper()

	data := testedArchive.ReadFileContents(path)

	assert.DeepEqual(t, data, []byte(expectedContents))
}

func assertArchiveIndex(t *testing.T, testedArchive *TestedArchive, expectedFiles []*archive.FileInfo) {
	t.Helper()

	archiveRW := testedArchive.OpenReadWriter()

	storedFiles, err := archiveRW.ListFiles()
	assert.NilError(t, err)
	assert.DeepEqual(t, storedFiles, expectedFiles)
}

var (
	newFileContent = []byte("new file contents")
	newFileSha256  = "428189279ce0f27c1e26d0555538043bae351115e3795b1d3ffcb2948de131dd"

	existingFile01Info = &archive.FileInfo{
		Path:   "existing-file-01",
		Length: 22,
		Digest: map[string]string{
			"SHA256": "2a5c7f36d8af12221b84b588566aa964668fb7fc0f5d717f906a25bbab8798d4",
		},
	}
	existingFile02Info = &archive.FileInfo{
		Path:   "existing-file-02",
		Length: 30,
		Digest: map[string]string{
			"SHA256": "e1bd0afc0d2276fab4d1458b12e01d760e0d0e2a7cf6f72fde281f297f8bff65",
		},
	}
	newFileInfo = &archive.FileInfo{
		Path:   "new-file",
		Length: 17,
		Digest: map[string]string{
			"SHA256": "428189279ce0f27c1e26d0555538043bae351115e3795b1d3ffcb2948de131dd",
		},
	}
)
