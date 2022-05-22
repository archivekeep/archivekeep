package cmd

import (
	"fmt"
	"strings"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/operations/compare"
)

func compareCmd() *cobra.Command {
	var connectionFlags ConnectionFlags

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}
		remoteArchive, err := openOtherArchive(cmd, args[0], currentArchive, connectionFlags)
		if err != nil {
			return fmt.Errorf("open remote archive: %w", err)
		}

		compareResult, err := compare.CompareArchives(currentArchive, remoteArchive)
		if err != nil {
			return fmt.Errorf("compare: %w", err)
		}

		printUnmatchedBaseExtras(cmd, compareResult, "local", "remote")
		printUnmatchedOtherExtras(cmd, compareResult, "local", "remote")
		printRelocations(cmd, compareResult)

		// TODO: show list of same filenames with different contents and other comparison results

		printStats(cmd, compareResult, "local", "remote")

		return nil
	}

	cmd := &cobra.Command{
		Use:   "compare [other archive location]",
		Short: "Compares current archive to remote archive",
		Args:  cobra.ExactArgs(1),
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&connectionFlags.insecure, "insecure", false, "use insecure connection")

	return cmd
}

func printUnmatchedBaseExtras(cmd *cobra.Command, compareResult compare.Result, baseName string, otherName string) {
	if len(compareResult.UnmatchedBaseExtras) > 0 {
		cmd.Printf("\nExtra %s files not matched to %s files:\n", baseName, otherName)
		for _, sourceExtra := range compareResult.UnmatchedBaseExtras {
			cmd.Printf("\t%s\n", filenamesPrint(sourceExtra.Filenames))
		}
	}
}

func printUnmatchedOtherExtras(cmd *cobra.Command, compareResult compare.Result, baseName string, otherName string) {
	if len(compareResult.UnmatchedOtherExtras) > 0 {
		cmd.Printf("\nExtra %s files not matched to %s files:\n", baseName, otherName)
		for _, targetExtra := range compareResult.UnmatchedOtherExtras {
			cmd.Printf("\t%s\n", filenamesPrint(targetExtra.Filenames))
		}
	}
}

func printRelocations(cmd *cobra.Command, compareResult compare.Result) {
	if len(compareResult.Relocations) > 0 {
		cmd.Printf("\nFiles to be moved:\n")
		for _, move := range compareResult.Relocations {
			cmd.Printf("\t%s -> %s\n", filenamesPrint(move.OriginalFileNames), filenamesPrint(move.NewFileNames))
		}
	}
}

func printStats(
	cmd *cobra.Command,
	compareResult compare.Result,
	baseName string,
	otherName string,
) {
	cmd.Printf("\n")

	cmd.Printf("Extra files in %s archive: %d\n", baseName, len(compareResult.UnmatchedBaseExtras))
	cmd.Printf("Extra files in %s archive: %d\n", otherName, len(compareResult.UnmatchedOtherExtras))
	cmd.Printf("Total files present in both archives: %d\n", len(compareResult.AllBaseFiles)-len(compareResult.UnmatchedBaseExtras))

	cmd.Printf("\n")
}

func filenamesPrint(filenames []string) string {
	if len(filenames) == 1 {
		return filenames[0]
	} else {
		return "{" + strings.Join(filenames, ", ") + "}"
	}
}
