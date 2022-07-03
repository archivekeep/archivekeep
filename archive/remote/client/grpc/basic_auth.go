package grpcarchive

import (
	"context"
	"encoding/base64"
)

type BasicAuth struct {
	Username string
	Password string

	AllowInsecureTransport bool
}

func (b BasicAuth) GetRequestMetadata(ctx context.Context, in ...string) (map[string]string, error) {
	auth := b.Username + ":" + b.Password
	enc := base64.StdEncoding.EncodeToString([]byte(auth))
	return map[string]string{
		"authorization": "Basic " + enc,
	}, nil
}

func (b BasicAuth) RequireTransportSecurity() bool {
	return !b.AllowInsecureTransport
}
