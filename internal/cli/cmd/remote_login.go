package cmd

import (
	"fmt"
	"time"

	"github.com/spf13/cobra"
	"google.golang.org/grpc"

	grpcarchive "github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/x/operations/remotes"
)

func remoteLoginCmd() *cobra.Command {
	var connectionFlags ConnectionFlags

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}

		remoteHostAddress := args[0]

		username := util.AskForInput(cmd, "Username")
		password, err := util.AskForPassword(cmd, "Password: ")
		if err != nil {
			return fmt.Errorf("get password")
		}

		personalAccessToken, err := createPersonalAccessToken(cmd, connectionFlags, username, password, remoteHostAddress)
		if err != nil {
			return fmt.Errorf("create personal access token: %w", err)
		}

		err = currentArchive.OpenKeyring(util.PasswordPrompt(cmd, "Enter keyring password: "))
		if err != nil {
			return fmt.Errorf("open keyring: %w", err)
		}

		return remotes.AddPATConnection(
			remoteHostAddress,
			personalAccessToken.Token,
			currentArchive.RemotesStore(),
		)
	}

	cmd := &cobra.Command{
		Use:   "login",
		Short: "Login into remote host (creates Personal Access Token)",
		RunE:  cmdFunc,

		Args: cobra.ExactArgs(1),
	}

	connectionFlags.register(cmd)

	return cmd
}

func createPersonalAccessToken(
	cmd *cobra.Command,
	connectionFlags ConnectionFlags,
	username string,
	password string,
	remoteHostAddress string,
) (*pb.PersonalAccessToken, error) {
	var options []grpc.DialOption

	if connectionFlags.insecure {
		options = append(options, grpc.WithInsecure())
	}

	options = append(options, grpc.WithPerRPCCredentials(grpcarchive.BasicAuth{
		Username: username,
		Password: password,

		AllowInsecureTransport: connectionFlags.insecure,
	}))

	grpcConnection, err := grpc.Dial(remoteHostAddress, options...)
	if err != nil {
		return nil, fmt.Errorf("dial grpc: %w", err)
	}
	defer grpcConnection.Close()

	personalAccessToken, err := pb.NewPersonalAccessTokenServiceClient(grpcConnection).CreatePersonalAccessToken(cmd.Context(), &pb.CreatePersonalAccessTokenRequest{
		Name: fmt.Sprintf("archivekeep cli (login) at %s", time.Now().Format(time.RFC3339)),
	})
	if err != nil {
		return nil, fmt.Errorf("call remote: %w", err)
	}

	return personalAccessToken, nil
}
