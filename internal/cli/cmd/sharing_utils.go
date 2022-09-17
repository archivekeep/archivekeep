package cmd

import (
	"fmt"
	paths "path"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/x/archive/wrapper/readonly"
	"github.com/archivekeep/archivekeep/x/operations/sharing"
)

func openForSharing(cmd *cobra.Command, allowWrite bool) (string, *sharing.SingleArchive, error) {
	currentArchive, err := currentarchive.OpenAndUnlockKeyring(util.PasswordPrompt(cmd, "Enter keyring password: "))
	if err != nil {
		return "", nil, fmt.Errorf("open current archive: %w", err)
	}
	currentArchiveName := paths.Base(currentArchive.Location())

	var currentArchiveAccessor archive.ReadWriter = currentArchive
	if !allowWrite {
		currentArchiveAccessor = &readonly.Archive{Base: currentArchiveAccessor}
	}

	sharingArchiveManager := sharing.NewSingleArchive(currentArchiveAccessor, currentArchive.Keyring())

	return currentArchiveName, sharingArchiveManager, nil
}
