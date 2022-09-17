package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/x/archive/wrapper/logged"
	"github.com/archivekeep/archivekeep/x/operations/comparison"
	"github.com/archivekeep/archivekeep/x/operations/sync"
)

func performSync(
	cmd *cobra.Command,
	options sync.Options,
	fromName, toName, operationName string,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	compareResult, err := comparison.Execute(fromArchive, toArchive)
	if err != nil {
		return fmt.Errorf("prepare sync: %w", err)
	}

	// TODO: execute sync.Plan(compareResult, options) and show the generated plan instead

	compareResult.PrintAll(cmd, fromName, toName)

	if len(compareResult.UnmatchedBaseExtras) == 0 && len(compareResult.Relocations) == 0 {
		cmd.Printf("no files to %s ...\n", operationName)
		return nil
	}

	if !util.AskForConfirmation(cmd, "Do you want to perform %s?", operationName) {
		return nil
	}

	cmd.Printf("\n")

	err = sync.PerformSync(
		cmd.Context(),
		options,
		compareResult,
		fromArchive,
		&logged.Archive{
			Base:     toArchive,
			Printfer: cmd,
		},
	)

	if err != nil {
		return fmt.Errorf("execute %s: %w", operationName, err)
	}

	return nil
}
