package api

import "context"

type PersonalAccessTokenService interface {
	CreatePersonalAccessToken(ctx context.Context, request *CreatePersonalAccessTokenRequest) (*PersonalAccessToken, error)
}

type CreatePersonalAccessTokenRequest struct {
	Name string
}

type PersonalAccessToken struct {
	ID             string
	Name           string
	Token          string
	TokenLastEight string
}
