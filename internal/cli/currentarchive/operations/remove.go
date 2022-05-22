package operations

import (
	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
)

type Remove struct {
	FileNames []string
}

func (remove Remove) Execute(
	cmd *cobra.Command, // TODO: get rid of cmd to allow GUI clients
	currentArchive currentarchive.CurrentArchive,
) error {
	filesToRemove := util.FindFilesByGlobs(remove.FileNames...)

	for _, file := range filesToRemove {
		if !currentArchive.Contains(file) {
			continue
		}

		err := currentArchive.Remove(file)
		if err != nil {
			cmd.Printf("failed to remove from archive: %v\n", err)
		}

		cmd.Printf("removed: %s\n", file)
	}

	return nil
}
