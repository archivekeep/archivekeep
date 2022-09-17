package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	cryptoarchive "github.com/archivekeep/archivekeep/archive/local/driver/crypto"
	"github.com/archivekeep/archivekeep/internal/util"
)

func keyringChangePassword() *cobra.Command {
	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := cryptoarchive.Open(".", util.PasswordPrompt(cmd, "Enter keyring password: "))
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		err = currentArchive.ChangeKeyringPassword(util.PasswordDoublePrompt(cmd, "Enter new password: "))
		if err != nil {
			return fmt.Errorf("change password: %w", err)
		}

		cmd.Printf("Archive password successfully changed\n")
		return nil
	}

	cmd := &cobra.Command{
		Use:   "change-password",
		Short: "Changes password of the keyring",
		RunE:  cmdFunc,
	}

	return cmd
}
