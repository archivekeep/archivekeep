package sharing_test

import (
	"testing"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
	"github.com/archivekeep/archivekeep/x/operations/sharing"
)

func TestArchiveManager_Init(t *testing.T) {
	_, archiveLocation := testarchive.CreateTestingArchive01(t)

	ca, err := currentarchive.OpenLocation(archiveLocation)
	assert.NilError(t, err)

	err = ca.OpenKeyring(fixedPasswordFunc)
	assert.NilError(t, err)

	archiveSharingManager := sharing.NewSingleArchive(ca, ca.Keyring())

	err = archiveSharingManager.Init()
	assert.NilError(t, err)

	// TODO: verify stored certificates

	err = archiveSharingManager.Init()
	assert.NilError(t, err)

	// TODO: verify stored certificates are same

	// TODO: corrupt stored configuration
	//err = archiveSharingManager.Init()
	//assert.Error(t, err, "stored certificates are invalid")
}

func Test(t *testing.T) {
	_, archiveLocation := testarchive.CreateTestingArchive01(t)

	archiveInstance, err := currentarchive.OpenLocation(archiveLocation)
	assert.NilError(t, err)

	err = archiveInstance.OpenKeyring(fixedPasswordFunc)
	assert.NilError(t, err)

	archiveSharingManager := sharing.NewSingleArchive(archiveInstance, archiveInstance.Keyring())

	err = archiveSharingManager.Init()
	assert.NilError(t, err)

	server, err := archiveSharingManager.Serve(
		"test-archive",
		true,
		false,
		"127.0.0.1",
		"",
	)
	assert.NilError(t, err)
	defer func() {
		server.GracefulStop()
		err := server.Wait()
		assert.NilError(t, err)
	}()

	newClientCertificate, err := archiveSharingManager.RegisterNewClient("test-client")
	assert.NilError(t, err)

	connectToken, err := remote.NewConnectToken(
		server.Addr().String(),
		archiveSharingManager.GetRootCertificate(),
		newClientCertificate,
	)
	assert.NilError(t, err)

	connection, err := connectToken.ToConnection()
	assert.NilError(t, err)

	grpArchive, err := openGrpcArchive(connection, "test-archive")
	assert.NilError(t, err)

	index, err := grpArchive.ListFiles()
	assert.NilError(t, err)
	assert.Equal(t, len(index), 6)

	nonExistingArchive, err := openGrpcArchive(connection, "unknown-archive")
	assert.NilError(t, err)

	_, err = nonExistingArchive.ListFiles()
	assert.ErrorContains(t, err, "get archive with name unknown-archive: not found")
}

func openGrpcArchive(connection *remote.Connection, archiveName string) (archive.ReadWriter, error) {
	conn, err := grpc.Dial(
		connection.Address,
		grpc.WithTransportCredentials(
			credentials.NewTLS(connection.ClientTLSConfig()),
		),
	)
	if err != nil {
		return nil, err
	}

	// TODO: defer conn.Close()

	return grpcarchive.NewRemoteArchive(conn, archiveName), nil
}

func fixedPasswordFunc() (string, error) {
	return "123-456-for-test", nil
}
