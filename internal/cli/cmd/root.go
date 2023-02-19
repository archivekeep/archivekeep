package cmd

import (
	"context"
	"fmt"
	"os"
	"os/signal"

	"github.com/spf13/cobra"
)

func New() *cobra.Command {
	rootCmd := &cobra.Command{
		Use:   "archivekeep",
		Short: "Archivekeep facilitates files backups and archiving",

		SilenceErrors: true,
		SilenceUsage:  true,

		DisableAutoGenTag: true,
	}

	// local archive manipulation
	rootCmd.AddCommand(initCmd())
	rootCmd.AddCommand(addCmd())
	rootCmd.AddCommand(rmCmd)
	rootCmd.AddCommand(statusCmd)
	rootCmd.AddCommand(verifyCmd())

	// synchronization of archives
	rootCmd.AddCommand(compareCmd())
	rootCmd.AddCommand(pullCmd())
	rootCmd.AddCommand(pushCmd())

	// util
	rootCmd.AddCommand(genMarkdownCmd())
	rootCmd.AddCommand(versionCmd)

	// remotes management
	remoteCmd := &cobra.Command{Use: "remote"}
	remoteCmd.AddCommand(remoteConnectCmd())
	remoteCmd.AddCommand(remoteLoginCmd())
	rootCmd.AddCommand(remoteCmd)

	// sharing
	sharingCmd := &cobra.Command{
		Use:    "sharing",
		Hidden: true,
		Short:  "This is EXPERIMENTAL sharing functionality",
	}
	sharingCmd.AddCommand(sharingInitCmd())
	sharingCmd.AddCommand(sharingServeCmd())
	sharingCmd.AddCommand(sharingAddClientCmd())
	rootCmd.AddCommand(sharingCmd)

	// keyring
	keyringCmd := &cobra.Command{Use: "keyring"}
	keyringCmd.AddCommand(keyringChangePassword())
	rootCmd.AddCommand(keyringCmd)

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
