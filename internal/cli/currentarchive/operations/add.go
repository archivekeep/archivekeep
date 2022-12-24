package operations

import (
	"fmt"
	"os"
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

	DisableMovesCheck bool
}

type Move struct {
	From, To string
}

type AddPrepareResult struct {
	NewFiles []string

	Moves []Move

	MissingFiles []string
}

func (add Add) Prepare(
	currentArchive currentarchive.CurrentArchive,
) (AddPrepareResult, error) {
	var result AddPrepareResult

	filesMatchingPattern := currentArchive.FindFilesFromWD(add.FileNames)

	if !add.DisableMovesCheck {
		existingFiles := util.FindFilesByGlobs(".")
		sort.Strings(existingFiles)

		storedFiles, err := currentArchive.StoredFiles()
		if err != nil {
			return AddPrepareResult{}, fmt.Errorf("retrieve current archive stored files: %w", err)
		}
		sort.Strings(storedFiles)

		missingFilesByChecksum := map[string]string{}
		for _, inArchiveFullPath := range storedFiles {
			cwdRelativePath, err := filepath.Rel(currentArchive.WorkingSubdirectory, inArchiveFullPath)
			if err != nil {
				return AddPrepareResult{}, fmt.Errorf("reconstruct archive relative path to working directory for %s: %w", cwdRelativePath, err)
			}

			if !util.FileExists(cwdRelativePath) {
				checksum, err := currentArchive.FileChecksum(inArchiveFullPath)
				if err != nil {
					return AddPrepareResult{}, fmt.Errorf("get checksum of missing file %s: %w", cwdRelativePath, err)
				}

				missingFilesByChecksum[checksum] = inArchiveFullPath
			}
		}

		for _, inArchiveFullPath := range filesMatchingPattern {
			cwdRelativePath, err := filepath.Rel(currentArchive.WorkingSubdirectory, inArchiveFullPath)
			if err != nil {
				return AddPrepareResult{}, fmt.Errorf("reconstruct archive relative path to working directory for %s: %w", inArchiveFullPath, err)
			}

			currentDir, _ := os.Getwd()
			fmt.Printf("cwd: %s\n", currentDir)

			checksum, err := util.ComputeChecksum(afero.NewOsFs(), cwdRelativePath)
			if err != nil {
				return AddPrepareResult{}, fmt.Errorf("compute checksum of %s: %w", cwdRelativePath, err)
			}

			if missing, found := missingFilesByChecksum[checksum]; found {
				result.Moves = append(result.Moves, Move{
					From: missing,
					To:   inArchiveFullPath,
				})
				delete(missingFilesByChecksum, checksum)
			} else {
				if !currentArchive.Contains(inArchiveFullPath) {
					result.NewFiles = append(result.NewFiles, inArchiveFullPath)
				}
			}
		}

		for _, path := range missingFilesByChecksum {
			result.MissingFiles = append(result.MissingFiles, path)
		}

		sort.Slice(result.Moves, func(i, j int) bool {
			return strings.Compare(result.Moves[i].From, result.Moves[j].From) < 0
		})
	}

	return result, nil
}

func (result AddPrepareResult) PrintSummary(
	cmd *cobra.Command, // TODO: get rid of cmd to allow GUI clients
	currentArchive currentarchive.CurrentArchive,
) error {
	if len(result.MissingFiles) > 0 {
		cmd.Printf("Missing indexed files not matched by add:\n")
		sort.Strings(result.MissingFiles)

		for _, file := range result.MissingFiles {
			relToWD, err := filepath.Rel(currentArchive.WorkingSubdirectory, file)
			if err != nil {
				return fmt.Errorf("resolve relative path to archive root: %w", err)
			}
			cmd.Printf("\t%s\n", relToWD)
		}
		cmd.Printf("\n")
	}

	if len(result.NewFiles) > 0 {
		cmd.Printf("New files to be indexed:\n")
		sort.Strings(result.NewFiles)
		for _, file := range result.NewFiles {
			relToWD, err := filepath.Rel(currentArchive.WorkingSubdirectory, file)
			if err != nil {
				return fmt.Errorf("resolve relative path to archive root: %w", err)
			}
			cmd.Printf("\t%s\n", relToWD)
		}
		cmd.Printf("\n")
	}

	if len(result.Moves) > 0 {
		cmd.Printf("Files to be moved:\n")
		for _, move := range result.Moves {
			relToWDFrom, err := filepath.Rel(currentArchive.WorkingSubdirectory, move.From)
			if err != nil {
				return fmt.Errorf("resolve relative path to archive root: %w", err)
			}
			relToWDTo, err := filepath.Rel(currentArchive.WorkingSubdirectory, move.To)
			if err != nil {
				return fmt.Errorf("resolve relative path to archive root: %w", err)
			}
			cmd.Printf("\t%s -> %s\n", relToWDFrom, relToWDTo)
		}
		cmd.Printf("\n")
	}

	return nil
}

func (result AddPrepareResult) Execute(
	cmd *cobra.Command, // TODO: get rid of cmd to allow GUI clients
	currentArchive currentarchive.CurrentArchive,
) error {
	if len(result.Moves) > 0 && util.AskForConfirmation(cmd, "\nDo want to perform move?") {
		cmd.Printf("\nproceeding ...\n")

		for _, move := range result.Moves {
			err := currentArchive.Add(move.To)
			if err != nil {
				return fmt.Errorf("add new location of %s: %w", move.To, err)
			}

			err = currentArchive.Remove(move.From)
			if err != nil {
				return fmt.Errorf("remove old location %s (new %s): %w", move.From, move.To, err)
			}

			cmd.Printf("moved: %s -> %s\n", move.From, move.To)
		}
	}

	for _, file := range result.NewFiles {
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
