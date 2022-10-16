package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/x/operations/verification"
)

func verifyCmd() *cobra.Command {
	var flags struct {
		again bool
	}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		_, err = verification.Execute(
			cmd.Context(),
			currentArchive,
			verification.ActionOptions{
				Logger: cmd,

				StartFromScratch: flags.again,
			},
		)

		// TODO: fmt.Printf("Verification result: %v", result)

		return err
	}

	cmd := &cobra.Command{
		Use:   "verify",
		Short: "Verifies integrity archive",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&flags.again, "again", false, "verify it again")

	return cmd
}
