package tlsutil

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/pem"
	"fmt"
)

func LoadTLSCertificate(certificatePEM string, keyPEM string) (*tls.Certificate, error) {
	cert, err := tls.X509KeyPair([]byte(certificatePEM), []byte(keyPEM))
	if err != nil {
		return nil, fmt.Errorf("parse key pair: %w", err)
	}

	cert.Leaf, err = x509.ParseCertificate(cert.Certificate[0])
	if err != nil {
		return nil, fmt.Errorf("parse certificate: %w", err)
	}

	return &cert, nil
}

func LoadX509Certificate(certificatePEM string) (*x509.Certificate, error) {
	pemBlock, _ := pem.Decode([]byte(certificatePEM))
	if pemBlock == nil {
		return nil, fmt.Errorf("PEM block is empty")
	}

	cert, err := x509.ParseCertificate(pemBlock.Bytes)
	if err != nil {
		return nil, fmt.Errorf("parse key pair: %w", err)
	}

	return cert, nil
}
