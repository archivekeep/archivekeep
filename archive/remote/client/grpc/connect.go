package grpcarchive

import (
	"context"
	"fmt"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/archivekeep/archivekeep/archive/remote"
	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	grpc_ak "github.com/archivekeep/archivekeep/server/api/grpc"
)

func Connect(connection *remote.Connection, archiveResourceName string) (*RemoteArchive, error) {
	var options []grpc.DialOption

	if connection.Secrets.ClientCertificate != nil {
		options = append(options, grpc.WithTransportCredentials(credentials.NewTLS(connection.ClientTLSConfig())))
	}

	if connection.Insecure {
		options = append(options, grpc.WithInsecure())
	}

	if connection.BasicAuthCredentials != nil {
		options = append(options, grpc.WithPerRPCCredentials(BasicAuth{
			Username: connection.BasicAuthCredentials.Username,
			Password: connection.BasicAuthCredentials.Password,

			AllowInsecureTransport: connection.Insecure,
		}))
	}

	grpcConnection, err := grpc.Dial(connection.Address, options...)
	if err != nil {
		return nil, fmt.Errorf("dial grpc: %w", err)
	}

	_, err = pb.NewArchiveServiceClient(grpcConnection).GetArchive(context.TODO(), &pb.GetArchiveRequest{
		Name: archiveResourceName,
	})
	if err != nil {
		return nil, fmt.Errorf("check archive access: %w", err)
	}

	archiveName, _ := grpc_ak.ArchiveResourceName(archiveResourceName).DeconstructParts()

	return NewRemoteArchive(grpcConnection, archiveName), nil
}
