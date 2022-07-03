package cli

import (
	"fmt"

	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/server/application"
	"github.com/spf13/cobra"
)

func manageUsersCreateCmd() *cobra.Command {
	// TODO: load
	config := application.Config{}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		app, err := application.CreateServer(config)
		if err != nil {
			return fmt.Errorf("initialize application: %w", err)
		}

		username := util.AskForInput(cmd, "Username")

		password, err := util.AskForPassword(cmd, "Password: ")
		if err != nil {
			return fmt.Errorf("get password: %w", err)
		}

		_, err = app.UserService.CreateUser(username, password)
		if err != nil {
			return fmt.Errorf("create user failed: %w", err)
		}

		return nil
	}

	cmd := &cobra.Command{
		Use:   "create",
		Short: "Create new user",
		RunE:  cmdFunc,
	}

	return cmd
}
