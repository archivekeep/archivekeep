package cmd

import (
	"fmt"
	"strings"

	"github.com/spf13/afero"
	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive"
	cryptoarchive "github.com/archivekeep/archivekeep/archive/local/driver/crypto"
	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
)

type ConnectionFlags struct {
	insecure bool
}

func openOtherArchive(
	cmd *cobra.Command,
	location string,
	currentArchive currentarchive.CurrentArchive,
	flags ConnectionFlags,
) (archive.ReadWriter, error) {
	if strings.HasPrefix(location, remotePrefix) {
		return openGrpcArchive(cmd, currentArchive, location, flags)
	}

	return openFileSystemArchive(cmd, location)
}

func openFileSystemArchive(cmd *cobra.Command, location string) (archive.ReadWriter, error) {
	var arw archive.ReadWriter
	var err error

	fs, sublocation, err := resolve(location)
	if err != nil {
		return nil, fmt.Errorf("open location: %w", err)
	}

	// TODO: open archive descriptor and resolve type in more deterministic way

	if arw, err = cryptoarchive.Open(location, util.PasswordPrompt(cmd, "Enter keyring password: ")); err == nil {
		return arw, nil
	}

	if arw, err = filesarchive.Open(fs, sublocation); err == nil {
		return arw, nil
	}

	return nil, err
}

func resolve(location string) (afero.Fs, string, error) {
	if strings.HasPrefix(location, "ssh://") {
		return resolveSSH(location)
	}

	return afero.NewOsFs(), location, nil
}
