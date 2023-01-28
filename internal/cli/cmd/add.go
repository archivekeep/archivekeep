package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive/operations"
)

func addCmd() *cobra.Command {
	var flags struct {
		disableMovesCheck bool

		doNotPrintPreparationSummary bool
	}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		operation := operations.Add{
			SearchGlobs: currentArchive.PrependWorkingDirectoryToPaths(args),

			DisableMovesCheck: flags.disableMovesCheck,
		}

		preparedOperation, err := operation.Prepare(currentArchive, currentArchive.WorkingSubdirectory)
		if err != nil {
			return fmt.Errorf("prepare adding files: %w", err)
		}

		if !flags.doNotPrintPreparationSummary {
			err := preparedOperation.PrintSummary(cmd, currentArchive)
			if err != nil {
				return fmt.Errorf("print summary of prepared operation before execution")
			}
		}

		err = preparedOperation.Execute(cmd, currentArchive)
		if err != nil {
			return fmt.Errorf("execute operation: %w", err)
		}

		return nil
	}

	cmd := &cobra.Command{
		Use:   "add",
		Short: "Adds files to archive",
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&flags.disableMovesCheck, "disable-moves-check", false, "do not check for moves or missing files")
	cmd.Flags().BoolVar(&flags.doNotPrintPreparationSummary, "do-not-print-preparation-summary", false, "do not print pre-execution summary")

	return cmd
}
