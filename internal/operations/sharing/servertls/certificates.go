package servertls

import (
	"bytes"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"net"
)

type Certificates struct {
	x509RootCertificate *x509.Certificate
	x509RootCertPool    *x509.CertPool

	tlsRootCertificate *tls.Certificate
	tlsServerCert      *tls.Certificate

	clientCertificates map[string]*x509.Certificate
}

func (certs *Certificates) ServerTLSConfig() *tls.Config {
	return &tls.Config{
		Certificates: []tls.Certificate{*certs.tlsServerCert},

		VerifyConnection: func(state tls.ConnectionState) error {
			return certs.verifyClient(state.PeerCertificates[0])
		},

		ClientAuth: tls.RequireAndVerifyClientCert,
		ClientCAs:  certs.x509RootCertPool,
	}
}

func (certs *Certificates) verifyClient(providedCertificate *x509.Certificate) error {
	clientName := providedCertificate.Subject.CommonName
	clientCert := certs.clientCertificates[clientName]

	if clientCert == nil {
		return fmt.Errorf("client %s was removed", clientName)
	}

	if bytes.Compare(clientCert.Raw, providedCertificate.Raw) != 0 {
		return fmt.Errorf("client %s used old certificate", clientName)
	}

	return nil
}

func (certs *Certificates) GetRootCertificate() *x509.Certificate {
	return certs.x509RootCertificate
}

func (certs *Certificates) RegisterNewClient(clientName string) (*tls.Certificate, error) {
	clientCertificate, err := certs.generateClientCertificate(clientName)
	if err != nil {
		return nil, fmt.Errorf("generate certificate: %w", err)
	}

	if certs.clientCertificates == nil {
		certs.clientCertificates = map[string]*x509.Certificate{}
	}

	certs.clientCertificates[clientName] = clientCertificate.Leaf

	return clientCertificate, nil
}

func (certs *Certificates) generateClientCertificate(clientName string) (*tls.Certificate, error) {
	clientCert, err := makeClientCertificate(*certs.tlsRootCertificate, clientName)
	if err != nil {
		return nil, err
	}

	err = verifyCertificate(certs.x509RootCertPool, clientCert.Leaf, x509.ExtKeyUsageClientAuth)
	if err != nil {
		return nil, err
	}

	return clientCert, nil
}

func GenerateCertificates() (*Certificates, error) {
	rootCert, rootCertX, err := genOwnCARoot()
	if err != nil {
		return nil, err
	}

	certpool := x509.NewCertPool()
	certpool.AddCert(rootCert.Leaf)

	err = verifyCertificate(certpool, rootCert.Leaf, x509.ExtKeyUsageServerAuth, x509.ExtKeyUsageClientAuth)
	if err != nil {
		return nil, err
	}

	serverCert, err := makeServerCert(*rootCert, []net.IP{net.ParseIP("127.0.0.1")})
	if err != nil {
		return nil, err
	}

	err = verifyCertificate(certpool, rootCert.Leaf, x509.ExtKeyUsageServerAuth)
	if err != nil {
		return nil, err
	}

	return &Certificates{
		x509RootCertificate: rootCertX,
		x509RootCertPool:    certpool,

		tlsRootCertificate: rootCert,

		tlsServerCert: serverCert,
	}, nil
}
