package akcf

import (
	"crypto/cipher"
	"fmt"
	"io"
)

type seekableDecryptStream struct {
	cipher cipher.Block

	baseReader io.ReadSeeker
	baseOffset int64

	streamReader io.Reader
}

func (s *seekableDecryptStream) Read(p []byte) (n int, err error) {
	return s.streamReader.Read(p)
}

func (s *seekableDecryptStream) Seek(offset int64, whence int) (int64, error) {
	if whence != io.SeekStart {
		return -1, fmt.Errorf("only io.SeekStart is supported")
	}

	cipherBlockSize := int64(s.cipher.BlockSize())
	ivBlockOffset := s.baseOffset + (offset/cipherBlockSize)*cipherBlockSize
	_, err := s.baseReader.Seek(ivBlockOffset, io.SeekStart)
	if err != nil {
		return -1, fmt.Errorf("seek reader of encrypted data: %w", err)
	}

	s.streamReader, err = createCFBStream(s.baseReader, s.cipher)
	if err != nil {
		return -1, fmt.Errorf("create decrypt stream: %w", err)
	}

	var bytesToDiscard [128]byte
	numBytesToDiscard := offset % cipherBlockSize
	discardedBytes, err := s.streamReader.Read(bytesToDiscard[0:numBytesToDiscard])
	if err != nil {
		return -1, fmt.Errorf("discard extra initial bytes: %w", err)
	}
	if discardedBytes != int(numBytesToDiscard) {
		return -1, fmt.Errorf("discarded invalid number of bytes: %d instead of %d", discardedBytes, numBytesToDiscard)
	}

	return offset, nil
}

func createCFBStream(reader io.Reader, blockCipher cipher.Block) (*cipher.StreamReader, error) {
	cipherIV := make([]byte, blockCipher.BlockSize())
	_, err := reader.Read(cipherIV)
	if err != nil {
		return nil, fmt.Errorf("read IV: %w", err)
	}

	return &cipher.StreamReader{
		S: cipher.NewCFBDecrypter(blockCipher, cipherIV),
		R: reader,
	}, nil
}
