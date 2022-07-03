package grpc_ak

import (
	"context"

	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	"github.com/archivekeep/archivekeep/server/api"
)

type personalAccessTokenServiceServer struct {
	pb.UnimplementedPersonalAccessTokenServiceServer

	applicationService api.PersonalAccessTokenService
}

func NewPersonalAccessTokenServiceServer(
	applicationService api.PersonalAccessTokenService,
) pb.PersonalAccessTokenServiceServer {
	return &personalAccessTokenServiceServer{
		applicationService: applicationService,
	}
}

func (p *personalAccessTokenServiceServer) CreatePersonalAccessToken(ctx context.Context, request *pb.CreatePersonalAccessTokenRequest) (*pb.PersonalAccessToken, error) {
	result, err := p.applicationService.CreatePersonalAccessToken(ctx, &api.CreatePersonalAccessTokenRequest{
		Name: request.Name,
	})
	if err != nil {
		return nil, err
	}

	return &pb.PersonalAccessToken{
		Id:             result.ID,
		Name:           result.Name,
		Token:          result.Token,
		TokenLastEight: result.TokenLastEight,
	}, nil
}
