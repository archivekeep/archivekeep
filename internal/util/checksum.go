package util

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"

	"github.com/spf13/afero"
)

func ComputeChecksum(fs afero.Fs, path string) (string, error) {
	content, err := afero.ReadFile(fs, path)
	if err != nil {
		return "", fmt.Errorf("read file: %w", err)
	}

	return ComputeChecksumFromBytes(content), nil
}

func ComputeChecksumFromBytes(content []byte) string {
	hash := sha256.New()
	hash.Write(content)
	sum := hex.EncodeToString(hash.Sum(nil))

	return sum
}
