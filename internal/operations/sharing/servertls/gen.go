package servertls

import (
	"crypto"
	"crypto/ed25519"
	"crypto/rand"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"fmt"
	"math/big"
	"net"
	"time"
)

func genOwnCARoot() (*tls.Certificate, *x509.Certificate, error) {
	var rootTemplate = x509.Certificate{
		SerialNumber: big.NewInt(1),
		Subject: pkix.Name{
			Country:      []string{"SK"},
			Organization: []string{"SelfHosted ArchiveKeep"},
			CommonName:   "Root CA",
		},
		NotBefore:             time.Now().Add(-10 * time.Second),
		NotAfter:              time.Now().AddDate(10, 0, 0),
		KeyUsage:              x509.KeyUsageCertSign | x509.KeyUsageCRLSign,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth, x509.ExtKeyUsageClientAuth},
		BasicConstraintsValid: true,
		IsCA:                  true,
		MaxPathLen:            2,
	}

	publicKey, privateKey, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, nil, err
	}

	rootTLSCert, rootCert, _, err := genCert(&rootTemplate, &rootTemplate, publicKey, privateKey, privateKey)

	return rootTLSCert, rootCert, err
}

func makeServerCert(parentCert tls.Certificate, ipAddresses []net.IP) (*tls.Certificate, error) {
	serialNumberLimit := new(big.Int).Lsh(big.NewInt(1), 128)
	serialNumber, err := rand.Int(rand.Reader, serialNumberLimit)
	if err != nil {
		return nil, err
	}

	template := &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Country:      []string{"SK"},
			Organization: []string{"SelfHosted ArchiveKeep"},
			CommonName:   "Server",
		},
		NotBefore:             time.Now().Add(-10 * time.Second),
		NotAfter:              time.Now().Add(time.Hour),
		KeyUsage:              x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
		BasicConstraintsValid: true,
		IPAddresses:           ipAddresses,
	}

	return generateChildCert(parentCert, template)
}

func makeClientCertificate(parentCert tls.Certificate, clientName string) (*tls.Certificate, error) {
	serialNumberLimit := new(big.Int).Lsh(big.NewInt(1), 128)
	serialNumber, err := rand.Int(rand.Reader, serialNumberLimit)
	if err != nil {
		return nil, err
	}

	template := &x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Country:      []string{"SK"},
			Organization: []string{"SelfHosted ArchiveKeep Client"},
			CommonName:   clientName,
		},
		NotBefore:             time.Now().Add(-10 * time.Second),
		NotAfter:              time.Now().Add(7 * 24 * time.Hour),
		KeyUsage:              x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth},
		BasicConstraintsValid: true,
	}

	return generateChildCert(parentCert, template)
}

func generateChildCert(parent tls.Certificate, childTemplate *x509.Certificate) (*tls.Certificate, error) {
	publicKey, privateKey, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, err
	}

	tlsCert, _, _, err := genCert(childTemplate, parent.Leaf, publicKey, privateKey, parent.PrivateKey)

	return tlsCert, err
}

func genCert(
	template, parent *x509.Certificate,
	ownPublicKey crypto.PublicKey,
	ownPrivateKey, parentPrivateKey crypto.PrivateKey,
) (*tls.Certificate, *x509.Certificate, []byte, error) {
	certBytes, err := x509.CreateCertificate(rand.Reader, template, parent, ownPublicKey, parentPrivateKey)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("create certificate: %w", err)
	}

	cert, err := x509.ParseCertificate(certBytes)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("parse certificate: %w", err)
	}

	certPEM := pem.Block{Type: "CERTIFICATE", Bytes: certBytes}
	certPEMBytes := pem.EncodeToMemory(&certPEM)

	return &tls.Certificate{
		Certificate: [][]byte{certBytes},
		Leaf:        cert,
		PrivateKey:  ownPrivateKey,
	}, cert, certPEMBytes, nil
}

func verifyCertificate(certPool *x509.CertPool, c *x509.Certificate, keyUsages ...x509.ExtKeyUsage) error {
	if len(keyUsages) == 0 {
		return fmt.Errorf("no key usages specified")
	}

	for _, keyUsage := range keyUsages {
		opts := x509.VerifyOptions{
			Roots:     certPool,
			KeyUsages: []x509.ExtKeyUsage{keyUsage},
		}

		if _, err := c.Verify(opts); err != nil {
			return err
		}
	}

	return nil
}
