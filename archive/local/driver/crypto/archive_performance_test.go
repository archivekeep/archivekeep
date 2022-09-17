package cryptoarchive

import (
	"math/rand"
	"runtime"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/benchmark/datasizes"
)

func BenchmarkArchive_FileChecksum(b *testing.B) {
	datasizes.Run(b, func(b *testing.B, sizeInBytes int64) {
		currentRawData := make([]byte, sizeInBytes)
		_, err := rand.Read(currentRawData)
		assert.NilError(b, err)

		_, archive := createTempArchiveWithContents(b, map[string]string{})
		err = archive.SaveFileFromBytes(currentRawData, "test-file")
		assert.NilError(b, err)

		runtime.GC()

		b.ResetTimer()

		for i := 0; i < b.N; i++ {
			_, err := archive.FileChecksum("test-file")
			assert.NilError(b, err)
		}

	})
}
