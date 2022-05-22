package operations

import (
	"fmt"
	"path/filepath"
	"sort"
	"strings"

	"github.com/spf13/afero"
	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
)

type Add struct {
	FileNames []string

	ResolveMoves bool
}

func (add Add) Execute(
	cmd *cobra.Command, // TODO: get rid of cmd to allow GUI clients
	currentArchive currentarchive.CurrentArchive,
) error {
	filesToAdd := currentArchive.FindFilesFromWD(add.FileNames)

	if add.ResolveMoves {
		existingFiles := util.FindFilesByGlobs(".")
		sort.Strings(existingFiles)

		storedFiles, err := currentArchive.StoredFiles()
		if err != nil {
			return fmt.Errorf("retrieve current archive stored files: %w", err)
		}
		sort.Strings(storedFiles)

		missingFilesByChecksum := map[string]string{}
		for _, path := range storedFiles {
			if !util.FileExists(path) {
				checksum, err := currentArchive.FileChecksum(path)
				if err != nil {
					panic(err)
				}

				missingFilesByChecksum[checksum] = path
			}
		}

		type Move struct{ from, to string }

		var newUnmatchedFiles []string
		var missingUnmatchedFiles []string
		var moves []Move

		for _, path := range filesToAdd {
			checksum, err := util.ComputeChecksum(afero.NewOsFs(), path)
			if err != nil {
				panic(err)
			}

			if missing, found := missingFilesByChecksum[checksum]; found {
				moves = append(moves, Move{
					from: missing,
					to:   path,
				})
				delete(missingFilesByChecksum, checksum)
			} else {
				if !currentArchive.Contains(path) {
					newUnmatchedFiles = append(newUnmatchedFiles, path)
				}
			}
		}

		for _, path := range missingFilesByChecksum {
			missingUnmatchedFiles = append(missingUnmatchedFiles, path)
		}

		if len(missingUnmatchedFiles) > 0 {
			cmd.Printf("\nMissing files not matched by add:\n")
			sort.Strings(missingUnmatchedFiles)
			for _, file := range missingUnmatchedFiles {
				cmd.Printf("\t%s\n", file)
			}
		}

		if len(newUnmatchedFiles) > 0 {
			cmd.Printf("\nNew files not matched to missing files:\n")
			sort.Strings(newUnmatchedFiles)
			for _, file := range newUnmatchedFiles {
				cmd.Printf("\t%s\n", file)
			}
		}

		if len(moves) > 0 {
			cmd.Printf("\nFiles to be moved:\n")
			sort.Slice(moves, func(i, j int) bool {
				return strings.Compare(moves[i].from, moves[j].from) < 0
			})
			for _, move := range moves {
				cmd.Printf("\t%s -> %s\n", move.from, move.to)
			}
		}

		if util.AskForConfirmation(cmd, "\nDo want to perform move?") {
			cmd.Printf("\nproceeding ...\n")

			for _, move := range moves {
				err := currentArchive.Add(move.to)
				if err != nil {
					panic(err)
				}

				err = currentArchive.Remove(move.from)
				if err != nil {
					panic(err)
				}

				cmd.Printf("moved: %s -> %s\n", move.from, move.to)
			}

			filesToAdd = newUnmatchedFiles
		} else {
			return nil
		}
	}

	for _, file := range filesToAdd {
		if currentArchive.Contains(file) {
			continue
		}

		err := currentArchive.Add(file)
		if err != nil {
			cmd.Printf("failed to add: %v\n", err)
		}

		relToWD, err := filepath.Rel(currentArchive.WorkingSubdirectory, file)
		if err != nil {
			return fmt.Errorf("resolve relative path to archive root: %w", err)
		}

		cmd.Printf("added: %s\n", relToWD)
	}

	return nil
}
