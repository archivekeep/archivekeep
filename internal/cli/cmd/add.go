package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive/operations"
)

func addCmd() *cobra.Command {
	var flags struct {
		resolveMoves bool
	}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		operation := operations.Add{
			FileNames: args,

			ResolveMoves: flags.resolveMoves,
		}

		return operation.Execute(cmd, currentArchive)
	}

	cmd := &cobra.Command{
		Use:   "add",
		Short: "Adds files to archive",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&flags.resolveMoves, "resolve-moves", false, "resolves added files against checksums of missing files")

	return cmd
}
