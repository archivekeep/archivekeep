package akcf

import (
	"crypto/aes"
	"crypto/cipher"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"

	"github.com/keybase/saltpack"
)

type DecryptOptions struct {
	Keyring    saltpack.Keyring
	SigKeyring saltpack.SigKeyring
}

type FileInfo struct {
	PublicMetadata  PublicMetadata
	PrivateMetadata PrivateMetadata
}

func Decrypt(reader io.Reader, options *DecryptOptions) (*FileInfo, []byte, error) {
	fileInfo, aesCipher, err := decryptBase(reader, options)
	if err != nil {
		return nil, nil, err
	}

	cipherReader, err := createCFBStream(reader, aesCipher)
	if err != nil {
		return nil, nil, fmt.Errorf("create decrypt stream: %w", err)
	}

	result, err := ioutil.ReadAll(cipherReader)
	if err != nil {
		return nil, nil, fmt.Errorf("read content: %w", err)
	}
	privateMetadata := fileInfo.PrivateMetadata
	if int64(len(result)) != privateMetadata.Data.Length {
		return nil, nil, fmt.Errorf("data length %d differs to bytes in data %d", privateMetadata.Data.Length, len(result))
	}

	return fileInfo, result, nil
}

func decryptBase(reader io.Reader, options *DecryptOptions) (*FileInfo, cipher.Block, error) {
	_, err := io.CopyN(io.Discard, reader, int64(len(constructHeader())))
	if err != nil {
		return nil, nil, fmt.Errorf("skip header failed")
	}

	var metadataLength int32
	err = binary.Read(reader, binary.BigEndian, &metadataLength)
	if err != nil {
		return nil, nil, fmt.Errorf("read metadata length: %w", err)
	}

	metadataRaw := make([]byte, metadataLength)
	_, err = reader.Read(metadataRaw)
	if err != nil {
		return nil, nil, fmt.Errorf("read encrypted metadata: %w", err)
	}

	publicMetadata, privateMetadata, err := decryptDeserializeMetadata(metadataRaw, options)
	if err != nil {
		return nil, nil, fmt.Errorf("extract metadata: %w", err)
	}

	cipherKey, err := privateMetadata.Encryption.KeyBytes()
	if err != nil {
		return nil, nil, fmt.Errorf("get cipher key: %w", err)
	}

	aesCipher, err := aes.NewCipher(cipherKey)
	if err != nil {
		return nil, nil, fmt.Errorf("create aes cipher: %w", err)
	}

	return &FileInfo{
		PublicMetadata:  publicMetadata,
		PrivateMetadata: privateMetadata,
	}, aesCipher, nil
}

func DecryptSeekableStream(reader io.ReadSeeker, options *DecryptOptions) (*FileInfo, io.ReadSeeker, error) {
	fileInfo, aesCipher, err := decryptBase(reader, options)
	if err != nil {
		return nil, nil, err
	}

	currentPosition, err := reader.Seek(0, io.SeekCurrent)
	if err != nil {
		return nil, nil, fmt.Errorf("get start offset of encrypted data: %w", err)
	}

	cipherReader := &seekableDecryptStream{
		cipher: aesCipher,

		baseReader: reader,
		baseOffset: currentPosition,
	}
	_, err = cipherReader.Seek(0, io.SeekStart)
	if err != nil {
		return nil, nil, fmt.Errorf("initiate sync: %w", err)
	}

	return fileInfo, cipherReader, nil
}

func decryptDeserializeMetadata(
	raw []byte,
	options *DecryptOptions,
) (PublicMetadata, PrivateMetadata, error) {
	publicMetadataLength := int32(binary.BigEndian.Uint32(raw[0:4]))
	privateMetadataLength := int32(binary.BigEndian.Uint32(raw[publicMetadataLength+4 : publicMetadataLength+8]))

	publicMetadataStart := int32(4)
	publicMetadataEnd := publicMetadataStart + publicMetadataLength
	privateMetadataStart := publicMetadataEnd + 4
	privateMetadataEnd := privateMetadataStart + privateMetadataLength

	_, publicMetadataJsonBytes, err := saltpack.Verify(
		saltpack.CheckKnownMajorVersion,
		raw[publicMetadataStart:publicMetadataEnd],
		options.SigKeyring,
	)
	if err != nil {
		return PublicMetadata{}, PrivateMetadata{}, fmt.Errorf("verify: %w", err)
	}

	_, privateMetadataJsonBytes, err := saltpack.Open(
		saltpack.CheckKnownMajorVersion,
		raw[privateMetadataStart:privateMetadataEnd],
		options.Keyring,
	)
	if err != nil {
		return PublicMetadata{}, PrivateMetadata{}, fmt.Errorf("decrypt and verify: %w", err)
	}

	var publicMetadata PublicMetadata
	err = json.Unmarshal(publicMetadataJsonBytes, &publicMetadata)
	if err != nil {
		return PublicMetadata{}, PrivateMetadata{}, fmt.Errorf("unmarshal: %w", err)
	}

	var privateMetadata PrivateMetadata
	err = json.Unmarshal(privateMetadataJsonBytes, &privateMetadata)
	if err != nil {
		return PublicMetadata{}, PrivateMetadata{}, fmt.Errorf("unmarshal: %w", err)
	}

	return publicMetadata, privateMetadata, nil
}
