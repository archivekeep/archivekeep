package akcf_test

import (
	"testing"

	akcf "github.com/archivekeep/archivekeep/crypto/file"
	"github.com/keybase/saltpack/basic"
	"gotest.tools/v3/assert"
)

func createTestOptionsFromRandomKeys(tb testing.TB) (*akcf.EncryptOptions, *akcf.DecryptOptions) {
	keyring := basic.NewKeyring()

	boxKey, err := keyring.GenerateBoxKey()
	assert.NilError(tb, err)

	signingKey, err := keyring.GenerateSigningKey()
	assert.NilError(tb, err)

	encryptOptions := &akcf.EncryptOptions{
		BoxKey:     boxKey,
		SigningKey: signingKey,
	}

	decryptOptions := &akcf.DecryptOptions{
		Keyring:    keyring,
		SigKeyring: keyring,
	}

	return encryptOptions, decryptOptions
}
