package akcf

import (
	"encoding/base64"
)

const (
	fileFormatBasicSaltpack = 1
)

type PublicMetadata struct {
	Data DataMetadata
}

type PrivateMetadata struct {
	Encryption EncryptionMetadata
	Data       DataMetadata
}

type EncryptionMetadata struct {
	Cipher string
	Key    string
}

func (m EncryptionMetadata) KeyBytes() ([]byte, error) {
	return base64.StdEncoding.DecodeString(m.Key)
}

type DataMetadata struct {
	Length int64
	Digest map[string]string
}

func constructHeader() []byte {
	return append(
		[]byte("ArchiveKeep Encrypted File"),
		0,
		fileFormatBasicSaltpack,
	)
}
