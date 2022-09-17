package akcf_test

import (
	"bytes"
	"io"
	"io/ioutil"
	"math/rand"
	"testing"

	"gotest.tools/v3/assert"

	akcf "github.com/archivekeep/archivekeep/crypto/file"
)

func TestHeader(t *testing.T) {
	encryptOptions, _ := createTestOptionsFromRandomKeys(t)

	textToEncrypt := []byte("1234568790abcdef")

	var out bytes.Buffer
	err := akcf.Encrypt(
		&out,
		bytes.NewReader(textToEncrypt),
		int64(len(textToEncrypt)),
		map[string]string{"A": "b"},
		encryptOptions,
	)
	assert.NilError(t, err)
	assert.DeepEqual(t, out.Bytes()[0:28], []byte("ArchiveKeep Encrypted File\x00\x01"))
}

func TestEncryptionDecryption(t *testing.T) {
	encryptOptions, decryptOptions := createTestOptionsFromRandomKeys(t)

	originalData := make([]byte, 20000)
	_, err := rand.Read(originalData)
	assert.NilError(t, err)

	var out bytes.Buffer
	err = akcf.Encrypt(
		&out,
		bytes.NewReader(originalData),
		int64(len(originalData)),
		map[string]string{"A": "b"},
		encryptOptions,
	)
	assert.NilError(t, err)

	encryptedBytes := out.Bytes()

	t.Run("Decrypt", func(t *testing.T) {
		_, decryptedBytes, err := akcf.Decrypt(bytes.NewReader(encryptedBytes), decryptOptions)
		assert.NilError(t, err)
		assert.DeepEqual(t, decryptedBytes, originalData)
	})

	t.Run("DecryptSeekable_ReadAll", func(t *testing.T) {
		_, decryptStream, err := akcf.DecryptSeekableStream(bytes.NewReader(encryptedBytes), decryptOptions)
		assert.NilError(t, err)

		decryptedBytes, err := ioutil.ReadAll(decryptStream)
		assert.NilError(t, err)
		assert.DeepEqual(t, decryptedBytes, originalData)
	})

	t.Run("DecryptSeekable_Seeking", func(t *testing.T) {
		_, decryptStream, err := akcf.DecryptSeekableStream(bytes.NewReader(encryptedBytes), decryptOptions)
		assert.NilError(t, err)

		for i := len(originalData) - 1; i >= 0; i-- {
			_, err := decryptStream.Seek(int64(i), io.SeekStart)
			assert.NilError(t, err)

			var b [1]byte
			n, err := decryptStream.Read(b[:])
			assert.NilError(t, err)
			assert.DeepEqual(t, n, 1)
			assert.DeepEqual(t, originalData[i], b[0])
		}
	})
}
