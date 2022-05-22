package akcf

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io"

	"github.com/keybase/saltpack"
)

type EncryptOptions struct {
	BoxKey     saltpack.BoxSecretKey
	SigningKey saltpack.SigningSecretKey
}

func Encrypt(
	out io.Writer,
	dataReader io.Reader,
	dataLength int64,
	dataDigest map[string]string,
	options *EncryptOptions,
) error {
	_, err := out.Write(constructHeader())
	if err != nil {
		return err
	}

	key := make([]byte, 16)
	if _, err := io.ReadFull(rand.Reader, key); err != nil {
		return fmt.Errorf("generate random key: %w", err)
	}

	dataMetadata := DataMetadata{
		Length: dataLength,
		Digest: dataDigest,
	}

	publicMetadata := PublicMetadata{
		Data: dataMetadata,
	}

	privateMetadata := PrivateMetadata{
		Encryption: EncryptionMetadata{
			Cipher: "AES-128-CFB",
			Key:    base64.StdEncoding.EncodeToString(key),
		},

		Data: dataMetadata,
	}

	metadataSerializedEncrypted, err := serializeEncryptMetadata(
		publicMetadata,
		privateMetadata,
		options,
	)
	if err != nil {
		return fmt.Errorf("metadata: %w", err)
	}

	// TODO: check int32 overflow - unlikely to happen, not impossible
	err = binary.Write(out, binary.BigEndian, int32(len(metadataSerializedEncrypted)))
	if err != nil {
		return err
	}

	_, err = out.Write(metadataSerializedEncrypted)
	if err != nil {
		return err
	}

	aesCipher, err := aes.NewCipher(key)
	if err != nil {
		return fmt.Errorf("create aes cipher: %w", err)
	}

	iv := make([]byte, aesCipher.BlockSize())
	if _, err := io.ReadFull(rand.Reader, iv); err != nil {
		return fmt.Errorf("generate random iv: %w", err)
	}
	_, err = out.Write(iv)
	if err != nil {
		return err
	}

	cipherStream := cipher.NewCFBEncrypter(aesCipher, iv)

	cipherWriter := &cipher.StreamWriter{
		S: cipherStream,
		W: out,
	}

	bytesWritten, err := io.Copy(cipherWriter, dataReader)
	if err != nil {
		return fmt.Errorf("write cipherdata: %w", err)
	}
	if bytesWritten != dataLength {
		return fmt.Errorf("data length %d differs to bytes in data stream %d", dataLength, bytesWritten)
	}

	return nil
}

func serializeEncryptMetadata(
	publicMetadata PublicMetadata,
	privateMetadata PrivateMetadata,
	options *EncryptOptions,
) ([]byte, error) {
	publicMetadataJson, err := json.Marshal(publicMetadata)
	if err != nil {
		return nil, fmt.Errorf("marshal: %w", err)
	}

	privateMetadataJson, err := json.Marshal(privateMetadata)
	if err != nil {
		return nil, fmt.Errorf("marshal: %w", err)
	}

	signedPublicMetadata, err := saltpack.Sign(
		saltpack.Version2(),
		publicMetadataJson,
		options.SigningKey,
	)
	if err != nil {
		return nil, fmt.Errorf("sign: %w", err)
	}

	encryptedPrivateMetadata, err := saltpack.Seal(
		saltpack.Version2(),
		privateMetadataJson,
		options.BoxKey,
		[]saltpack.BoxPublicKey{options.BoxKey.GetPublicKey()},
	)
	if err != nil {
		return nil, fmt.Errorf("encrypt and sign: %w", err)
	}

	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, int32(len(signedPublicMetadata)))
	buf.Write(signedPublicMetadata)
	binary.Write(&buf, binary.BigEndian, int32(len(encryptedPrivateMetadata)))
	buf.Write(encryptedPrivateMetadata)

	return buf.Bytes(), nil
}
