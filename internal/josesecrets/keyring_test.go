package josesecrets_test

import (
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

func Test(t *testing.T) {
	testDir := t.TempDir()

	keyringFileName := paths.Join(testDir, "keyring")
	keyringPassword := "123-456"

	initialKeyring, err := josesecrets.OpenKeyring(keyringFileName, keyringPassword)
	assert.NilError(t, err)
	updateKeyring(t, initialKeyring)
	verifyContents(t, initialKeyring)

	reopenedKeyring, err := josesecrets.OpenKeyring(keyringFileName, keyringPassword)
	assert.NilError(t, err)
	verifyContents(t, reopenedKeyring)
}

func updateKeyring(t *testing.T, initialKeyring *josesecrets.Keyring) {
	var err error

	err = initialKeyring.Put("username", "someone")
	assert.NilError(t, err)

	err = initialKeyring.Put("password", "super secret password")
	assert.NilError(t, err)

	err = initialKeyring.Put("metadata", metadata{
		DisplayName: "SomeOne",
		Order:       42,
		Hash:        []byte{0, 1, 2, 3, 4, 5, 'A', 'B', 'C', 240, 255},
	})
	assert.NilError(t, err)
}

func verifyContents(t *testing.T, k *josesecrets.Keyring) {
	var err error

	var result struct {
		Username, Password string
		metadata           metadata
	}

	err = k.GetTo("username", &result.Username)
	assert.NilError(t, err)
	assert.Equal(t, result.Username, "someone")

	err = k.GetTo("password", &result.Username)
	assert.NilError(t, err)
	assert.Equal(t, result.Username, "super secret password")

	err = k.GetTo("metadata", &result.metadata)
	assert.NilError(t, err)
	assert.DeepEqual(t, result.metadata, metadata{
		DisplayName: "SomeOne",
		Order:       42,
		Hash:        []byte{0, 1, 2, 3, 4, 5, 'A', 'B', 'C', 240, 255},
	})
}

type metadata struct {
	DisplayName string
	Order       int
	Hash        []byte
}
