package cli

import (
	"fmt"
	"log"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/server/application"
)

func runCmd() *cobra.Command {
	var options application.Config

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		app, err := application.CreateServer(options)
		if err != nil {
			return fmt.Errorf("create server: %w", err)
		}

		listeners, err := options.Listen()
		if err != nil {
			return fmt.Errorf("listen: %w", err)
		}

		for _, listener := range listeners.HTTP.Listeners {
			log.Printf("listening HTTP on: %s", listener.Addr().String())
		}
		for _, listener := range listeners.GRPC.Listeners {
			log.Printf("listening GRPC on: %s", listener.Addr().String())
		}

		return app.Serve(cmd.Context(), listeners)
	}

	cmd := &cobra.Command{
		Use:   "run",
		Short: "Run server",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&options.GRPC.Enable, "grpc-enable", true, "enable GRPC API")
	cmd.Flags().BoolVar(&options.GRPC.ListenOnAllInterfaces, "grpc-listen-on-all-interfaces", false, "listen on all interfaces")
	cmd.Flags().Uint16Var(&options.GRPC.Port, "grpc-port", 24202, "port to listen GRPC on")

	cmd.Flags().BoolVar(&options.HTTP.Enable, "http-enable", true, "enable HTTP API")
	cmd.Flags().Uint16Var(&options.HTTP.Port, "http-port", 4202, "port to listen HTTP on")
	cmd.Flags().BoolVar(&options.HTTP.ListenOnAllInterfaces, "http-listen-on-all-interfaces", false, "listen on all interfaces")
	cmd.Flags().StringVar(&options.HTTP.PublicPath, "http-public-path", "", "relative path to root of web app ")

	return cmd
}
