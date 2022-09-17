package cmd

import (
	"fmt"
	"path/filepath"
	"sort"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
)

var statusCmd = &cobra.Command{
	Use:   "status [globs]",
	Short: "Prints status of archive",
	RunE: func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		var matchedFiles []string
		if len(args) > 0 {
			matchedFiles = currentArchive.FindFilesFromWD(args)
		} else {
			matchedFiles = currentArchive.FindFiles()
		}

		var newFiles, storedFiles []string
		for _, path := range matchedFiles {
			if currentArchive.Contains(path) {
				storedFiles = append(storedFiles, path)
			} else {
				newFiles = append(newFiles, path)
			}
		}

		if len(newFiles) > 0 {
			cmd.Printf("\nFiles not added to the archive:\n")
			sort.Strings(newFiles)
			for _, file := range newFiles {
				relToWD, err := filepath.Rel(currentArchive.WorkingSubdirectory, file)
				if err != nil {
					return fmt.Errorf("resolve relative path to archive root: %w", err)
				}
				cmd.Printf("\t%s\n", relToWD)
			}
			cmd.Printf("\n")
		}

		if len(args) > 0 {
			cmd.Printf("Files in archive matching globs: %d\n", len(storedFiles))
		} else {
			cmd.Printf("Total files in archive: %d\n", len(storedFiles))
		}

		return nil
	},
}
