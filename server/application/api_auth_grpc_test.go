package application_test

import (
	"context"
	"testing"

	"google.golang.org/grpc"
	"gotest.tools/v3/assert"

	grpcarchive "github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
)

func Test_GRPCTokenAuth(t *testing.T) {
	server, listeners, closeServer := createTestServer(t)
	defer closeServer()

	testUserName := "test.user@localhost"
	testUserPassword := "test-password"

	_, err := server.UserService.CreateUser(
		testUserName,
		testUserPassword,
	)
	assert.NilError(t, err)

	t.Run("Create and use token", func(t *testing.T) {
		grpcBasicPasswordConn, err := grpc.Dial(
			listeners.GRPC.Listeners[0].Addr().String(),
			grpc.WithInsecure(),
			grpc.WithPerRPCCredentials(grpcarchive.BasicAuth{
				Username: testUserName,
				Password: testUserPassword,

				AllowInsecureTransport: true,
			}),
		)
		defer grpcBasicPasswordConn.Close()
		assert.NilError(t, err)

		personalAccessTokenServiceClient := pb.NewPersonalAccessTokenServiceClient(grpcBasicPasswordConn)
		result, err := personalAccessTokenServiceClient.CreatePersonalAccessToken(context.TODO(), &pb.CreatePersonalAccessTokenRequest{
			Name: "Test token",
		})
		assert.NilError(t, err)

		grpcPersonalAccessTokenConn, err := grpc.Dial(
			listeners.GRPC.Listeners[0].Addr().String(),
			grpc.WithInsecure(),
			grpc.WithPerRPCCredentials(grpcarchive.BasicAuth{
				Username: testUserName,
				Password: result.Token,

				AllowInsecureTransport: true,
			}),
		)
		defer grpcBasicPasswordConn.Close()

		archiveServiceClientViaToken := pb.NewArchiveServiceClient(grpcPersonalAccessTokenConn)
		_, err = archiveServiceClientViaToken.ListArchives(context.TODO(), &pb.ListArchivesRequest{})
		assert.NilError(t, err)
	})
}
