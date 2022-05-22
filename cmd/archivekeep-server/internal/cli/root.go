package cli

import (
	"context"
	"fmt"
	"os"
	"os/signal"

	"github.com/spf13/cobra"
)

func New() *cobra.Command {
	rootCmd := &cobra.Command{
		Use:   "archivekeep-server",
		Short: "Server application for Archivekeep",

		SilenceErrors: true,
		SilenceUsage:  true,
	}

	rootCmd.AddCommand(runCmd())

	// manage archives
	manageArchivesCmd := &cobra.Command{Use: "archives"}
	manageArchivesCmd.AddCommand(manageArchivesCreateCmd())

	// manage users
	manageUsersCmd := &cobra.Command{Use: "users"}
	manageUsersCmd.AddCommand(manageUsersCreateCmd())

	// manage
	manageCmd := &cobra.Command{Use: "manage"}
	manageCmd.AddCommand(manageArchivesCmd)
	manageCmd.AddCommand(manageUsersCmd)
	rootCmd.AddCommand(manageCmd)

	return rootCmd
}

func Execute() {
	osInterrupt := make(chan os.Signal, 1)
	signal.Notify(osInterrupt, os.Interrupt)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	go func() {
		<-osInterrupt
		cancel()
	}()

	cmd := New()

	if err := cmd.ExecuteContext(ctx); err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: %v\n", err)
		os.Exit(1)
	}
}
