package application

import (
	"context"
	"encoding/base64"
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

	user, err := getUser(server, authHeaders[0])
	if err != nil {
		return ctx, err
	}

	mdPurged := md.Copy()
	mdPurged["authorization"] = nil

	ctx = userIDContext(ctx, user.ID)
	ctx = metadata.NewIncomingContext(ctx, mdPurged)

	return ctx, nil
}

func getUser(server *Server, authHeader string) (*User, error) {
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
		if len(cs) != 2 {
			return nil, fmt.Errorf("invalid format: extract username and password from: %s", cs)
		}
		userName, userPassword := cs[0], cs[1]

		return server.UserService.VerifyLogin(context.TODO(), userName, userPassword)
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
