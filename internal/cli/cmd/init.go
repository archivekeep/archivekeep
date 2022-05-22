package cmd

import (
	"fmt"

	"github.com/spf13/afero"
	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive/local/driver/crypto"
	"github.com/archivekeep/archivekeep/archive/local/driver/plain"
	"github.com/archivekeep/archivekeep/internal/util"
)

func initCmd() *cobra.Command {
	var flags struct {
		encrypted bool
	}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		if !flags.encrypted {
			_, err := filesarchive.Init(afero.NewOsFs(), ".")

			if err != nil {
				return fmt.Errorf("initialization failed: %w", err)
			}
		} else {
			_, err := cryptoarchive.Create(".", util.PasswordDoublePrompt(cmd, "Enter keyring password: "))

			if err != nil {
				return fmt.Errorf("initialization of encrypted archive failed: %w", err)
			}
		}

		cmd.Printf("Archive successfully initialized")
		return nil
	}

	cmd := &cobra.Command{
		Use:   "init",
		Short: "Initializes new archive",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&flags.encrypted, "encrypted", false, "initializes archive storing files in encrypted form")

	return cmd
}
