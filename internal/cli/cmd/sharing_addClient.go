package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/internal/util"
)

func sharingAddClientCmd() *cobra.Command {
	cmdFunc := func(cmd *cobra.Command, args []string) error {
		_, sharingArchiveManager, err := openForSharing(cmd, false)
		if err != nil {
			return err
		}

		clientName := util.AskForInput(cmd, "Enter client name")

		tls, err := sharingArchiveManager.RegisterNewClient(clientName)
		if err != nil {
			return fmt.Errorf("register new client: %w", err)
		}

		token, err := remote.NewConnectToken(
			"127.0.0.1:24202", // TODO: resolve own addr
			sharingArchiveManager.GetRootCertificate(),
			tls,
		)
		if err != nil {
			return fmt.Errorf("create token: %w", err)
		}

		tokenBase64, err := token.EncodeBase64()
		if err != nil {
			return fmt.Errorf("encode token: %w", err)
		}

		cmd.Printf("Token:\n\n%s\n\n", tokenBase64)

		return nil
	}

	cmd := &cobra.Command{
		// TODO: sharing client add
		Use:   "add-client",
		Short: "Add client to sharing",
		RunE:  cmdFunc,
	}

	return cmd
}
