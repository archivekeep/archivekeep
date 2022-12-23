package remote

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/internal/tlsutil"
)

type BasicAuthCredentials struct {
	Username, Password string
}

type Connection struct {
	Address string
	Secrets ConnectionSecrets

	BasicAuthCredentials *BasicAuthCredentials

	Insecure bool
}

type ConnectionSecrets struct {
	ServerRootCertificate *x509.Certificate
	ClientCertificate     *tls.Certificate
}

func (c *Connection) ClientTLSConfig() *tls.Config {
	rootCertPool := x509.NewCertPool()
	rootCertPool.AddCert(c.Secrets.ServerRootCertificate)

	return &tls.Config{
		GetClientCertificate: func(info *tls.CertificateRequestInfo) (*tls.Certificate, error) {
			return c.Secrets.ClientCertificate, nil
		},
		RootCAs: rootCertPool,
	}
}

type connectionJSON struct {
	Address string

	RootCertificate string

	ClientCertificate string
	ClientPrivateKey  string

	BasicAuthCredentials *BasicAuthCredentials
}

func (c *Connection) UnmarshalJSON(bytes []byte) error {
	var jsonRepresentation connectionJSON

	err := json.Unmarshal(bytes, &jsonRepresentation)
	if err != nil {
		return err
	}

	rootCertificate, err := tlsutil.LoadX509Certificate(jsonRepresentation.RootCertificate)
	if err != nil {
		return fmt.Errorf("load root certificate: %w", err)
	}

	clientCertificate, err := tlsutil.LoadTLSCertificate(jsonRepresentation.ClientCertificate, jsonRepresentation.ClientPrivateKey)
	if err != nil {
		return fmt.Errorf("load client certificate: %w", err)
	}

	*c = Connection{
		Address: jsonRepresentation.Address,
		Secrets: ConnectionSecrets{
			ServerRootCertificate: rootCertificate,
			ClientCertificate:     clientCertificate,
		},
		BasicAuthCredentials: jsonRepresentation.BasicAuthCredentials,
	}

	return nil
}

func (c *Connection) MarshalJSON() ([]byte, error) {
	var err error

	jsonRepresentation := connectionJSON{
		Address:              c.Address,
		BasicAuthCredentials: c.BasicAuthCredentials,
	}

	if c.Secrets.ServerRootCertificate != nil {
		jsonRepresentation.RootCertificate = tlsutil.EncodeCertificatePEM(c.Secrets.ServerRootCertificate.Raw)
	}
	if c.Secrets.ClientCertificate != nil {
		jsonRepresentation.ClientCertificate = tlsutil.EncodeCertificatePEM(c.Secrets.ClientCertificate.Leaf.Raw)
		jsonRepresentation.ClientPrivateKey, err = tlsutil.EncodeKeyPEM(c.Secrets.ClientCertificate.PrivateKey)
		if err != nil {
			return nil, err
		}
	}

	return json.Marshal(jsonRepresentation)
}
