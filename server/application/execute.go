package application

import (
	"context"
	"errors"
	"net"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
	"golang.org/x/sync/errgroup"

	"github.com/archivekeep/archivekeep/server/api/grpc"
)

func (s *Server) Run(ctx context.Context) error {
	g, gCtx := errgroup.WithContext(ctx)

	if s.Config.HTTP.Enable {
		launchRestAPI(
			s,
			g,
			gCtx,
		)
	}

	if s.Config.GRPC.Enable {
		launchGrpcAPI(s, g, gCtx)
	}

	return g.Wait()
}

func launchRestAPI(app *Server, g *errgroup.Group, gCtx context.Context) {
	r := createRouter(app)

	ln, err := net.Listen("tcp", ":3000")
	if err != nil {
		panic(err)
	}

	httpServer := &http.Server{
		Addr:    ln.Addr().String(),
		Handler: r,
	}

	g.Go(func() error {
		err := httpServer.Serve(ln)

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

func launchGrpcAPI(app *Server, g *errgroup.Group, gCtx context.Context) {
	lis, err := net.Listen("tcp", ":24202")
	if err != nil {
		panic(err)
	}

	grpcServer := grpc_ak.NewServer(
		app.ArchiveRestService,
		unaryAuthInterceptor(app),
		streamAuthInterceptor(app),
	)

	g.Go(func() error {
		return grpcServer.Serve(lis)
	})

	go func() {
		<-gCtx.Done()

		grpcServer.GracefulStop()
	}()
}
