package cmd

import (
	"fmt"

	"github.com/spf13/cobra"
)

func sharingServeCmd() *cobra.Command {
	var flags struct {
		allowWrite bool

		apiGrpc         bool
		apiInsecureREST bool

		grpcHost string
		grpcPort string
	}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		if !flags.apiGrpc && !flags.apiInsecureREST {
			return fmt.Errorf("must specify at least one of --api-grpc or --api-insecure-rest")
		}

		currentArchiveName, sharingArchiveManager, err := openForSharing(cmd, flags.allowWrite)
		if err != nil {
			return err
		}

		server, err := sharingArchiveManager.Serve(
			currentArchiveName,
			flags.apiGrpc,
			flags.apiInsecureREST,
			flags.grpcHost,
			flags.grpcPort,
		)
		if err != nil {
			return fmt.Errorf("launch server: %w", err)
		}
		go func() {
			<-cmd.Context().Done()
			server.GracefulStop()
		}()

		cmd.Printf("Listening on %v\n\n", server.Addr())
		cmd.Printf("Available archives:\n - %s\n\n", currentArchiveName)

		return server.Wait()
	}

	cmd := &cobra.Command{
		Use:   "serve",
		Short: "Serves files",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&flags.allowWrite, "allow-write", false, "enables write access, default is read-only access")
	cmd.Flags().BoolVar(&flags.apiGrpc, "api-grpc", false, "enables GRPC API")
	cmd.Flags().BoolVar(&flags.apiInsecureREST, "api-insecure-rest", false, "enables REST API - INSECURE!!! ")
	cmd.Flags().StringVar(&flags.grpcHost, "grpc-host", "", "grpc host, defaults to empty - listen on all interfaces")
	cmd.Flags().StringVar(&flags.grpcPort, "grpc-port", "24202", "grpc port")

	return cmd
}
