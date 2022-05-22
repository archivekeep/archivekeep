package sharing

import (
	"context"
	"net"
	"net/http"

	"golang.org/x/sync/errgroup"
	"google.golang.org/grpc"
)

type Server struct {
	listener   net.Listener
	grpcServer *grpc.Server

	httpServer *http.Server

	serveGroup *errgroup.Group
}

func (s *Server) Addr() net.Addr {
	if s.listener == nil {
		return nil
	}

	return s.listener.Addr()
}

func (s *Server) Wait() error {
	return s.serveGroup.Wait()
}

func (s *Server) GracefulStop() {
	if s.grpcServer != nil {
		s.grpcServer.GracefulStop()
	}

	if s.httpServer != nil {
		s.httpServer.Shutdown(context.TODO())
	}
}
