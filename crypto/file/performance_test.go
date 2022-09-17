package akcf_test

import (
	"bytes"
	"math/rand"
	"runtime"
	"testing"

	"gotest.tools/v3/assert"

	akcf "github.com/archivekeep/archivekeep/crypto/file"
	"github.com/archivekeep/archivekeep/internal/tests/benchmark/datasizes"
)

func BenchmarkEncryption(b *testing.B) {
	encryptOptions, _ := createTestOptionsFromRandomKeys(b)

	datasizes.Run(b, func(b *testing.B, sizeInBytes int64) {
		currentRawData := make([]byte, sizeInBytes)
		_, err := rand.Read(currentRawData)
		assert.NilError(b, err)

		runtime.GC()

		b.SetBytes(sizeInBytes)
		b.ResetTimer()

		for i := 0; i < b.N; i++ {
			var out bytes.Buffer
			err := akcf.Encrypt(
				&out,
				bytes.NewReader(currentRawData),
				int64(len(currentRawData)),
				map[string]string{"A": "b"},
				encryptOptions,
			)
			assert.NilError(b, err)

			encryptedBytes := out.Bytes()

			b.ReportMetric(float64(len(currentRawData))/float64(len(encryptedBytes)), "plaintext/ciphertext")
		}
	})
}

func BenchmarkDecryption(b *testing.B) {
	encryptOptions, decryptOptions := createTestOptionsFromRandomKeys(b)

	datasizes.Run(b, func(b *testing.B, sizeInBytes int64) {
		currentRawData := make([]byte, sizeInBytes)
		_, err := rand.Read(currentRawData)
		assert.NilError(b, err)

		var out bytes.Buffer
		err = akcf.Encrypt(
			&out,
			bytes.NewReader(currentRawData),
			int64(len(currentRawData)),
			map[string]string{"A": "b"},
			encryptOptions,
		)
		assert.NilError(b, err)

		encryptedBytes := out.Bytes()

		runtime.GC()

		b.SetBytes(sizeInBytes)
		b.ResetTimer()

		for i := 0; i < b.N; i++ {
			_, decryptedBytes, err := akcf.Decrypt(bytes.NewReader(encryptedBytes), decryptOptions)
			assert.NilError(b, err)

			b.ReportMetric(float64(len(decryptedBytes))/float64(len(encryptedBytes)), "plaintext/ciphertext")
		}
	})
}
