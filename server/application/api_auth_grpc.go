package application

import (
	"context"
	"encoding/base64"
	"errors"
	"fmt"
	"strings"

	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

func unaryAuthInterceptor(app *Server) grpc.ServerOption {
	return grpc.UnaryInterceptor(
		func(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (resp interface{}, err error) {
			ctx, err = createAuthenticatedContext(app, ctx)
			if err != nil {
				return nil, err
			}

			return handler(ctx, req)
		},
	)
}

func streamAuthInterceptor(app *Server) grpc.ServerOption {
	return grpc.StreamInterceptor(
		func(srv interface{}, ss grpc.ServerStream, info *grpc.StreamServerInfo, handler grpc.StreamHandler) error {
			ctx, err := createAuthenticatedContext(app, ss.Context())
			if err != nil {
				return err
			}

			return handler(srv, &overrideServerStreamContext{
				ServerStream: ss,
				ctx:          ctx,
			})
		},
	)
}

func createAuthenticatedContext(server *Server, ctx context.Context) (context.Context, error) {
	md, _ := metadata.FromIncomingContext(ctx)
	authHeaders := md["authorization"]

	if len(authHeaders) != 1 {
		return ctx, nil
	}

	user, err := getUser(ctx, server, authHeaders[0])
	if err != nil {
		return ctx, err
	}

	mdPurged := md.Copy()
	mdPurged["authorization"] = nil

	ctx = NewContextWithAuthenticatedUser(ctx, user)
	ctx = metadata.NewIncomingContext(ctx, mdPurged)

	return ctx, nil
}

func getUser(ctx context.Context, server *Server, authHeader string) (*User, error) {
	authParts := strings.SplitN(authHeader, " ", 2)
	if len(authParts) != 2 {
		return nil, fmt.Errorf("invalid format: extract type from: %s", authHeader)
	}
	authType, authValue := authParts[0], authParts[1]

	switch authType {
	case "Basic":
		c, err := base64.StdEncoding.DecodeString(authValue)
		if err != nil {
			return nil, fmt.Errorf("invalid base64: %w", err)
		}

		cs := strings.SplitN(string(c), ":", 2)
		if len(cs) == 1 {
			user, err := server.PersonalAccessTokenApplicationService.TryTokenAuth(ctx, cs[0])
			if err != nil && !errors.Is(err, errDbNotExist) {
				return nil, fmt.Errorf("try token auth: %w", err)
			} else if err == nil {
				return user, nil
			}
		}

		if len(cs) != 2 {
			return nil, fmt.Errorf("invalid format: extract username and password from: %s", cs)
		}
		userName, passwordOrToken := cs[0], cs[1]

		user, err := server.PersonalAccessTokenApplicationService.TryTokenAuth(ctx, passwordOrToken)
		if err != nil && !(errors.Is(err, errDbNotExist) || strings.Contains(err.Error(), "too short token")) {
			return nil, fmt.Errorf("try token auth: %w", err)
		} else if err == nil {
			return user, nil
		}

		return server.UserService.VerifyLogin(context.TODO(), userName, passwordOrToken)
	default:
		return nil, fmt.Errorf("unsupported auth type: %s", authType)
	}
}

type overrideServerStreamContext struct {
	grpc.ServerStream

	ctx context.Context
}

func (o *overrideServerStreamContext) Context() context.Context {
	return o.ctx
}
