package tlsutil

import (
	"crypto"
	"crypto/x509"
	"encoding/pem"
)

func EncodeCertificatePEM(raw []byte) string {
	pemBlock := pem.Block{
		Type:  "CERTIFICATE",
		Bytes: raw,
	}

	return string(pem.EncodeToMemory(&pemBlock))
}

func EncodeKeyPEM(key crypto.PrivateKey) (string, error) {
	keyBytes, err := x509.MarshalPKCS8PrivateKey(key)
	if err != nil {
		return "", err
	}

	pemBlock := pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: keyBytes,
	}

	return string(pem.EncodeToMemory(&pemBlock)), nil
}
