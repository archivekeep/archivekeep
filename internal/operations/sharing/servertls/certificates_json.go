package servertls

import (
	"crypto/x509"
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/internal/tlsutil"
)

type certificatesJSON struct {
	RootCertificate string
	RootPrivateKey  string

	ServerCertificate string
	ServerPrivateKey  string

	ClientCertificates map[string]string
}

func (certs *Certificates) UnmarshalJSON(bytes []byte) error {
	var jsonRepresentation certificatesJSON

	err := json.Unmarshal(bytes, &jsonRepresentation)
	if err != nil {
		return err
	}

	rootCertificate, err := tlsutil.LoadTLSCertificate(jsonRepresentation.RootCertificate, jsonRepresentation.RootPrivateKey)
	if err != nil {
		return fmt.Errorf("load root certificate: %w", err)
	}

	serverCertificate, err := tlsutil.LoadTLSCertificate(jsonRepresentation.ServerCertificate, jsonRepresentation.ServerPrivateKey)
	if err != nil {
		return fmt.Errorf("load server certificate: %w", err)
	}

	certpool := x509.NewCertPool()
	certpool.AddCert(rootCertificate.Leaf)

	clientCertificates := map[string]*x509.Certificate{}
	for clientName, clientCertificate := range jsonRepresentation.ClientCertificates {
		clientCertificates[clientName], err = tlsutil.LoadX509Certificate(clientCertificate)
		if err != nil {
			return fmt.Errorf("load certificate for client %s: %w", clientName, err)
		}
	}

	*certs = Certificates{
		x509RootCertificate: rootCertificate.Leaf,
		x509RootCertPool:    certpool,

		tlsRootCertificate: rootCertificate,
		tlsServerCert:      serverCertificate,

		clientCertificates: clientCertificates,
	}

	return nil
}

func (certs *Certificates) MarshalJSON() ([]byte, error) {
	var err error

	jsonRepresentation := certificatesJSON{
		RootCertificate:   tlsutil.EncodeCertificatePEM(certs.x509RootCertificate.Raw),
		ServerCertificate: tlsutil.EncodeCertificatePEM(certs.tlsServerCert.Certificate[0]),

		ClientCertificates: map[string]string{},
	}

	jsonRepresentation.RootPrivateKey, err = tlsutil.EncodeKeyPEM(certs.tlsRootCertificate.PrivateKey)
	if err != nil {
		return nil, err
	}

	jsonRepresentation.ServerPrivateKey, err = tlsutil.EncodeKeyPEM(certs.tlsServerCert.PrivateKey)
	if err != nil {
		return nil, err
	}

	for clientName, clientCertificate := range certs.clientCertificates {
		jsonRepresentation.ClientCertificates[clientName] = tlsutil.EncodeCertificatePEM(clientCertificate.Raw)
	}

	return json.Marshal(jsonRepresentation)
}
