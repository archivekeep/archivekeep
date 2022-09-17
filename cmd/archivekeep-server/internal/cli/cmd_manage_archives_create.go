package cli

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/server/application"
)

func manageArchivesCreateCmd() *cobra.Command {
	// TODO: load
	config := application.Config{}

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		app, err := application.CreateServer(config)
		if err != nil {
			return fmt.Errorf("initialize application: %w", err)
		}

		owner := util.AskForInput(cmd, "Owner")
		name := util.AskForInput(cmd, "Name")

		user, err := app.UserRepository.GetUserByUserName(owner)
		if err != nil {
			return fmt.Errorf("get user with name '%s': %w", owner, err)
		}

		a, err := app.ArchiveService.CreateArchive(cmd.Context(), user.ResourceName(), name)
		if err != nil {
			return fmt.Errorf("create archive failed: %w", err)
		}

		cmd.Printf("Archive created: %v", a)

		return nil
	}

	cmd := &cobra.Command{
		Use:   "create",
		Short: "Create new archive",
		RunE:  cmdFunc,

		PersistentPreRun: func(cmd *cobra.Command, args []string) {
			cmd.Context()
		},
	}

	return cmd
}
