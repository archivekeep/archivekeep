package cmd

import (
	"encoding/base64"
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/operations/remotes"
	"github.com/archivekeep/archivekeep/internal/util"
)

func remoteConnectCmd() *cobra.Command {
	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		remoteHostName := args[0]

		tokenBase64 := util.AskForInput(cmd, "Enter received token")

		tokenBytes, err := base64.StdEncoding.DecodeString(tokenBase64)
		if err != nil {
			return fmt.Errorf("decode base64 token: %w", err)
		}

		err = currentArchive.OpenKeyring(util.PasswordPrompt(cmd, "Enter keyring password: "))
		if err != nil {
			return fmt.Errorf("open keyring: %w", err)
		}

		return remotes.AddConnection(
			remoteHostName,
			tokenBytes,
			currentArchive.Keyring(),
		)
	}

	cmd := &cobra.Command{
		Use:   "connect",
		Short: "Establishes connection to remote archive host",
		RunE:  cmdFunc,

		Args: cobra.ExactArgs(1),
	}

	return cmd
}
