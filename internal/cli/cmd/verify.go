package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/operations/verification"
)

var verifyCmd = &cobra.Command{
	Use:   "verify",
	Short: "Verifies integrity archive",
	RunE: func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		return verification.Verify(cmd.Context(), cmd, currentArchive)
	},
}
