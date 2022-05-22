package cli

import (
	"fmt"

	"github.com/archivekeep/archivekeep/server/application"
	"github.com/spf13/cobra"
)

func runCmd() *cobra.Command {
	var options application.Config

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		app, err := application.CreateServer(options)
		if err != nil {
			return fmt.Errorf("create server: %w", err)
		}

		return app.Run(cmd.Context())
	}

	cmd := &cobra.Command{
		Use:   "run",
		Short: "Run server",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&options.GRPC.Enable, "grpc-enable", true, "enable GRPC API")

	cmd.Flags().BoolVar(&options.HTTP.Enable, "http-enable", true, "enable HTTP API")
	cmd.Flags().StringVar(&options.HTTP.PublicPath, "http-public-path", "", "relative path to root of web app ")

	return cmd
}
