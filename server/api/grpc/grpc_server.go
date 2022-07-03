package grpc_ak

import (
	"google.golang.org/grpc"

	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	"github.com/archivekeep/archivekeep/server/api"
)

func NewServer(
	// TODO: pass extensible api.Application type instead of application services
	archiveService api.ArchiveService,
	personalAccessTokenService api.PersonalAccessTokenService,
	options ...grpc.ServerOption,
) *grpc.Server {
	grpcServer := grpc.NewServer(options...)
	pb.RegisterArchiveServiceServer(grpcServer, NewArchiveServiceServer(archiveService))
	pb.RegisterPersonalAccessTokenServiceServer(grpcServer, NewPersonalAccessTokenServiceServer(personalAccessTokenService))
	return grpcServer
}
