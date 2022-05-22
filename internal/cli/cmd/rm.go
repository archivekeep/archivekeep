package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive/operations"
)

var rmCmd = &cobra.Command{
	Use:   "rm",
	Short: "Remove files from archive",
	RunE: func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		operation := operations.Remove{
			FileNames: args,
		}

		return operation.Execute(cmd, currentArchive)
	},
}
