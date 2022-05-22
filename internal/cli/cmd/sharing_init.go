package cmd

import (
	"github.com/spf13/cobra"
)

func sharingInitCmd() *cobra.Command {
	cmdFunc := func(cmd *cobra.Command, args []string) error {
		_, sharingArchiveManager, err := openForSharing(cmd, false)
		if err != nil {
			return err
		}

		return sharingArchiveManager.Init()
	}

	cmd := &cobra.Command{
		Use:   "init",
		Short: "Initialize sharing server configuration",
		RunE:  cmdFunc,
	}

	return cmd
}
