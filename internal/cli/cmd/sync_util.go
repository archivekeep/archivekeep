package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/operations/compare"
	"github.com/archivekeep/archivekeep/internal/operations/sync"
	"github.com/archivekeep/archivekeep/internal/util"
)

func performSync(
	cmd *cobra.Command,
	options sync.Options,
	fromName, toName, operationName string,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	compareResult, err := compare.CompareArchives(fromArchive, toArchive)
	if err != nil {
		return fmt.Errorf("prepare sync: %w", err)
	}

	if len(compareResult.Relocations) > 0 {
		printUnmatchedBaseExtras(cmd, compareResult, fromName, toName)
		printUnmatchedOtherExtras(cmd, compareResult, toName, fromName)
		printRelocations(cmd, compareResult)

		if options.ResolveMoves && options.AdditiveDuplicating {
			return fmt.Errorf("use only one --resolve-moves or --additive-duplicating")
		}

		if options.AdditiveDuplicating {
			if util.AskForConfirmation(cmd, "\nDo want to %s additive duplicates?", operationName) {
				cmd.Printf("\nproceeding ...\n")

				err := sync.PerformAdditiveDuplicating(
					cmd.Context(),
					cmd,
					compareResult,
					fromArchive,
					toArchive,
				)

				if err != nil {
					return fmt.Errorf("%s changes: %w", operationName, err)
				}
			}
		} else if options.ResolveMoves {
			if util.AskForConfirmation(cmd, "\nDo want to %s file moves?", operationName) {
				cmd.Printf("\nproceeding ...\n")

				err := sync.PerformRelocationsSync(
					cmd.Context(),
					cmd,
					compareResult,
					options,
					fromArchive,
					toArchive,
				)

				if err != nil {
					return err
				}
			}
		} else {
			return fmt.Errorf("relocations detected, execute with --resolve-moves or --additive-duplicating")
		}
	}

	if len(compareResult.UnmatchedBaseExtras) > 0 {
		cmd.Printf("\nFiles to be %sed from %s to %s archive:\n", operationName, fromName, toName)
		for _, sourceExtra := range compareResult.UnmatchedBaseExtras {
			cmd.Printf("\t%s\n", filenamesPrint(sourceExtra.Filenames))
		}
	}

	printStats(cmd, compareResult, fromName, toName)

	if len(compareResult.UnmatchedBaseExtras) == 0 {
		cmd.Printf("no files to %s ...\n", operationName)
		return nil
	}

	if !util.AskForConfirmation(cmd, "Do you want to perform %s?", operationName) {
		return nil
	}

	cmd.Printf("\nproceeding ...\n")
	err = sync.PerformNewFilesSync(
		cmd.Context(),
		cmd,
		compareResult,
		fromArchive,
		toArchive,
	)
	if err != nil {
		return fmt.Errorf("%s new files: %w", operationName, err)
	}

	return nil
}
