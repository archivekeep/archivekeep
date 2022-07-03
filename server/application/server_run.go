package application

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
	"golang.org/x/sync/errgroup"

	"github.com/archivekeep/archivekeep/server/api/grpc"
)

func (s *Server) ListenAndServe(ctx context.Context) error {
	listeners, err := s.Config.Listen()
	if err != nil {
		return fmt.Errorf("listen: %w", err)
	}

	return s.Serve(ctx, listeners)
}

func (s *Server) Serve(ctx context.Context, listeners ServerListeners) error {
	g, gCtx := errgroup.WithContext(ctx)

	s.launchRestAPI(listeners.HTTP.Listeners, g, gCtx)
	s.launchGrpcAPI(listeners.GRPC.Listeners, g, gCtx)

	return g.Wait()
}

func (s *Server) launchRestAPI(listeners []net.Listener, g *errgroup.Group, gCtx context.Context) {
	r := createRouter(s)

	for _, _listener := range listeners {
		listener := _listener

		httpServer := &http.Server{
			Addr:    listener.Addr().String(),
			Handler: r,
		}

		g.Go(func() error {
			err := httpServer.Serve(listener)

			if !errors.Is(err, http.ErrServerClosed) {
				return err
			}

			return nil
		})

		go func() {
			<-gCtx.Done()

			httpServer.Shutdown(context.TODO())
		}()
	}
}

func (s *Server) launchGrpcAPI(listeners []net.Listener, g *errgroup.Group, gCtx context.Context) {
	for _, listener := range listeners {
		grpcServer := grpc_ak.NewServer(
			s.ArchiveApplicationService,
			s.PersonalAccessTokenApplicationService,
			unaryAuthInterceptor(s),
			streamAuthInterceptor(s),
		)

		g.Go(func() error {
			return grpcServer.Serve(listener)
		})

		go func() {
			<-gCtx.Done()

			grpcServer.GracefulStop()
		}()
	}
}
