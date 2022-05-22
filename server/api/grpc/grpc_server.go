package grpc_ak

import (
	"google.golang.org/grpc"

	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	"github.com/archivekeep/archivekeep/server/api"
)

func NewServer(
	archiveService api.ArchiveService,
	options ...grpc.ServerOption,
) *grpc.Server {
	grpcServer := grpc.NewServer(options...)
	pb.RegisterArchiveServiceServer(grpcServer, NewArchiveServiceServer(archiveService))
	return grpcServer
}
