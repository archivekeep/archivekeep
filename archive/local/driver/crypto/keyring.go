package cryptoarchive

import (
	"encoding/json"
	"fmt"

	"github.com/keybase/saltpack"
	"github.com/keybase/saltpack/basic"
)

type keyring struct {
	BoxKey     basic.SecretKey
	SigningKey basic.SigningSecretKey
}

func kidToPublicKey(kid []byte) basic.PublicKey {
	var publicKey basic.PublicKey
	copy(publicKey.RawBoxKey[:], kid)
	return publicKey
}

func (k keyring) CreateEphemeralKey() (saltpack.BoxSecretKey, error) {
	panic("implement me")
}

func (k keyring) LookupBoxSecretKey(kids [][]byte) (int, saltpack.BoxSecretKey) {
	for i, kid := range kids {
		if *k.BoxKey.GetRawPublicKey() == kidToPublicKey(kid).RawBoxKey {
			return i, k.BoxKey
		}
	}
	return -1, nil
}

func (k keyring) LookupBoxPublicKey(kid []byte) saltpack.BoxPublicKey {
	if *k.BoxKey.GetRawPublicKey() == kidToPublicKey(kid).RawBoxKey {
		return k.BoxKey.GetPublicKey()
	}
	return nil
}

func (k keyring) LookupSigningPublicKey(kid []byte) saltpack.SigningPublicKey {
	if *k.SigningKey.GetRawPublicKey() == kidToPublicKey(kid).RawBoxKey {
		return k.SigningKey.GetPublicKey()
	}
	return nil
}

func (k keyring) GetAllBoxSecretKeys() []saltpack.BoxSecretKey {
	panic("implement me")
}

func (k keyring) ImportBoxEphemeralKey(kid []byte) saltpack.BoxPublicKey {
	return kidToPublicKey(kid)
}

type keyPairJSON struct {
	Secret []byte
	Public []byte
}

type keyringJSON struct {
	BoxKey     keyPairJSON
	SigningKey keyPairJSON
}

func (k keyring) MarshalJSON() ([]byte, error) {
	return json.Marshal(keyringJSON{
		BoxKey: keyPairJSON{
			Secret: k.BoxKey.GetRawSecretKey()[:],
			Public: k.BoxKey.GetRawPublicKey()[:],
		},
		SigningKey: keyPairJSON{
			Secret: k.SigningKey.GetRawSecretKey()[:],
			Public: k.SigningKey.GetRawPublicKey()[:],
		},
	})
}

func (k *keyring) UnmarshalJSON(bytes []byte) error {
	var jsonValue keyringJSON

	err := json.Unmarshal(bytes, &jsonValue)
	if err != nil {
		return fmt.Errorf("unmarshall json: %w", err)
	}

	read := func(target, value []byte) {
		if len(value) != len(target) {
			err = fmt.Errorf("value doesn't have length of %d bytes, but %d", len(target), len(value))
		}
		copy(target[:], value[:])
	}

	decode32 := func(value []byte) *[32]byte {
		var result [32]byte
		read(result[:], value)
		return &result
	}

	decode64 := func(value []byte) *[64]byte {
		var result [64]byte
		read(result[:], value)
		return &result
	}

	keyring := keyring{
		BoxKey: basic.NewSecretKey(
			decode32(jsonValue.BoxKey.Public),
			decode32(jsonValue.BoxKey.Secret),
		),
		SigningKey: basic.NewSigningSecretKey(
			decode32(jsonValue.SigningKey.Public),
			decode64(jsonValue.SigningKey.Secret),
		),
	}

	if err == nil {
		*k = keyring
	}

	return err
}
