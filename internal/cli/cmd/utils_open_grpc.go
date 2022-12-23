package cmd

import (
	"fmt"
	"log"
	"strings"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/archive/remote"
	grpcarchive "github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
)

const (
	remotePrefix = "remote://"
)

func openGrpcArchive(
	cmd *cobra.Command,
	currentArchive currentarchive.CurrentArchive,
	location string, flags ConnectionFlags,
) (archive.ReadWriter, error) {
	parts := strings.SplitN(strings.TrimPrefix(location, remotePrefix), "/", 2)
	remoteName, archiveResourceName := parts[0], parts[1]

	connection, err := getConnectionConfiguration(cmd, flags, currentArchive, remoteName)
	if err != nil {
		return nil, fmt.Errorf("prepare connection configuration: %w", err)
	}

	grpcArchive, err := grpcarchive.Connect(connection, archiveResourceName)
	if err != nil && strings.Contains(err.Error(), "grpc: no transport security") {
		return nil, fmt.Errorf("flag --insecure is required to estabhlist insecure connection: %w", err)
	} else if err != nil && strings.HasSuffix(err.Error(), "not authorized") {
		return tryWithCredentials(cmd, connection, archiveResourceName)
	} else if err != nil {
		return nil, fmt.Errorf("grpc connect: %w", err)
	}

	return grpcArchive, nil
}

func getConnectionConfiguration(
	cmd *cobra.Command,
	flags ConnectionFlags,
	currentArchive currentarchive.CurrentArchive,
	remoteName string,
) (*remote.Connection, error) {
	err := currentArchive.OpenKeyring(util.PasswordPrompt(cmd, "Enter keyring password: "))
	if err != nil {
		return nil, fmt.Errorf("open keyring: %w", err)
	}

	var connection remote.Connection

	storedConnection, err := currentArchive.RemotesStore().Get(remoteName)
	if err != nil {
		return nil, fmt.Errorf("extract connection: %w", err)
	}
	if storedConnection != nil {
		connection = *storedConnection
		log.Printf("connection: %v\n", storedConnection)
	} else {
		log.Printf("WARNING: remote connection %s not registered, use remote login or remote connect", remoteName)

		connection = remote.Connection{
			Address: remoteName,

			Insecure: flags.insecure,
		}
	}

	if flags.insecure {
		connection.Insecure = true
	}

	return &connection, nil
}

func tryWithCredentials(
	cmd *cobra.Command,
	connection *remote.Connection,
	archiveResourceName string,
) (archive.ReadWriter, error) {
	username := util.AskForInput(cmd, "Username")
	password, err := util.AskForPassword(cmd, "Password: ")
	if err != nil {
		return nil, fmt.Errorf("get password")
	}

	grpcArchive, err := grpcarchive.Connect(
		&remote.Connection{
			Address: connection.Address,
			Secrets: connection.Secrets,

			Insecure: connection.Insecure,

			BasicAuthCredentials: &remote.BasicAuthCredentials{
				Username: strings.TrimSpace(username),
				Password: password,
			},
		},
		archiveResourceName,
	)
	if err != nil {
		return nil, fmt.Errorf("establish connection: %w", err)
	}

	return grpcArchive, nil
}
