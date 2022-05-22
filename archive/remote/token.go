package remote

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/internal/tlsutil"
)

type ConnectTokenJSON struct {
	Addr string

	RootCertificatePEM string

	ClientCertificatePEM        string
	ClientCertificatePrivateKey string
}

func (t ConnectTokenJSON) EncodeBase64() (string, error) {
	jsonBytes, err := json.Marshal(t)
	if err != nil {
		return "", fmt.Errorf("marshal to json: %w", err)
	}

	return base64.StdEncoding.EncodeToString(jsonBytes), nil
}

func NewConnectToken(
	addr string,
	RootCertificate *x509.Certificate,
	clientCertificate *tls.Certificate, // TODO: should be temporary client certificate
) (ConnectTokenJSON, error) {
	var err error

	tokenJson := ConnectTokenJSON{
		Addr: addr,
	}

	tokenJson.RootCertificatePEM = tlsutil.EncodeCertificatePEM(RootCertificate.Raw)
	tokenJson.ClientCertificatePEM = tlsutil.EncodeCertificatePEM(clientCertificate.Leaf.Raw)

	tokenJson.ClientCertificatePrivateKey, err = tlsutil.EncodeKeyPEM(clientCertificate.PrivateKey)
	if err != nil {
		return ConnectTokenJSON{}, fmt.Errorf("encode client key: %w", err)
	}

	return tokenJson, nil
}

func (t *ConnectTokenJSON) ToConnection() (*Connection, error) {
	secrets, err := parseSecrets(*t)
	if err != nil {
		return nil, fmt.Errorf("parse token secrets: %w", err)
	}

	return &Connection{
		Address: t.Addr,
		Secrets: secrets,
	}, nil
}

func parseSecrets(token ConnectTokenJSON) (ConnectionSecrets, error) {
	serverRootCertificate, err := tlsutil.LoadX509Certificate(
		token.RootCertificatePEM,
	)
	if err != nil {
		return ConnectionSecrets{}, fmt.Errorf("parse server root certificate: %w", err)
	}

	clientCertificate, err := tlsutil.LoadTLSCertificate(
		token.ClientCertificatePEM,
		token.ClientCertificatePrivateKey,
	)
	if err != nil {
		return ConnectionSecrets{}, fmt.Errorf("parse client certificate: %w", err)
	}

	return ConnectionSecrets{
		ServerRootCertificate: serverRootCertificate,
		ClientCertificate:     clientCertificate,
	}, nil
}
