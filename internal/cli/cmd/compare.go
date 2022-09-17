package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/x/operations/comparison"
)

func compareCmd() *cobra.Command {
	var connectionFlags ConnectionFlags

	cmdFunc := func(cmd *cobra.Command, args []string) error {
		currentArchive, err := currentarchive.Open()
		if err != nil {
			return fmt.Errorf("open current archive: %w", err)
		}
		remoteArchive, err := openOtherArchive(cmd, args[0], currentArchive, connectionFlags)
		if err != nil {
			return fmt.Errorf("open remote archive: %w", err)
		}

		compareResult, err := comparison.Execute(currentArchive, remoteArchive)
		if err != nil {
			return fmt.Errorf("compare: %w", err)
		}

		compareResult.PrintAll(cmd, "local", "remote")

		return nil
	}

	cmd := &cobra.Command{
		Use:   "compare [other archive location]",
		Short: "Compares current archive to remote archive",
		Args:  cobra.ExactArgs(1),
		RunE:  cmdFunc,
	}

	cmd.Flags().BoolVar(&connectionFlags.insecure, "insecure", false, "use insecure connection")

	return cmd
}
