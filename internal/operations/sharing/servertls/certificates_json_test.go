package servertls

import (
	"encoding/json"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

func TestJSONSerializationAndDeserialization(t *testing.T) {
	orig, err := GenerateCertificates()
	assert.NilError(t, err)

	jsonData, err := json.Marshal(orig)
	assert.NilError(t, err)
	t.Logf("JSON form: %s", string(jsonData))

	var loaded Certificates
	err = json.Unmarshal(jsonData, &loaded)
	assert.NilError(t, err)

	assert.DeepEqual(t, orig.x509RootCertificate.Raw, loaded.x509RootCertificate.Raw)
	assert.DeepEqual(t, orig.tlsRootCertificate.Leaf.Raw, loaded.tlsRootCertificate.Leaf.Raw)
	assert.DeepEqual(t, orig.tlsRootCertificate.PrivateKey, loaded.tlsRootCertificate.PrivateKey)

	assert.DeepEqual(t, orig.tlsServerCert.Leaf.Raw, loaded.tlsServerCert.Leaf.Raw)
	assert.DeepEqual(t, orig.tlsServerCert.PrivateKey, loaded.tlsServerCert.PrivateKey)

	// TODO: assert orig.clientCertificates
}

func TestPersistenceInKeyring(t *testing.T) {
	testDir := t.TempDir()

	keyringFileName := paths.Join(testDir, "keyring")
	keyringPassword := "123-456"

	orig, err := GenerateCertificates()
	assert.NilError(t, err)

	initialKeyring, err := josesecrets.OpenKeyring(keyringFileName, keyringPassword)
	assert.NilError(t, err)
	err = initialKeyring.Put("serverCertificates", orig)
	assert.NilError(t, err)
	verifyContents(t, orig, initialKeyring)

	reopenedKeyring, err := josesecrets.OpenKeyring(keyringFileName, keyringPassword)
	assert.NilError(t, err)
	verifyContents(t, orig, reopenedKeyring)
}

func verifyContents(t *testing.T, expected *Certificates, keyring *josesecrets.Keyring) {
	var loaded Certificates

	err := keyring.GetTo("serverCertificates", &loaded)
	assert.NilError(t, err)

	assert.DeepEqual(t, expected.x509RootCertificate.Raw, loaded.x509RootCertificate.Raw)
	assert.DeepEqual(t, expected.tlsRootCertificate.Leaf.Raw, loaded.tlsRootCertificate.Leaf.Raw)
	assert.DeepEqual(t, expected.tlsRootCertificate.PrivateKey, loaded.tlsRootCertificate.PrivateKey)

	assert.DeepEqual(t, expected.tlsServerCert.Leaf.Raw, loaded.tlsServerCert.Leaf.Raw)
	assert.DeepEqual(t, expected.tlsServerCert.PrivateKey, loaded.tlsServerCert.PrivateKey)

	// TODO: assert orig.clientCertificates
}
