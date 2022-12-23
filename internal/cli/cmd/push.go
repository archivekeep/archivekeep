package cmd

import (
	"fmt"

	"github.com/spf13/cobra"

	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/x/operations/sync"
)

func pushCmd() *cobra.Command {
	var flags struct {
		resolveMoves             bool
		enableDuplicateReduction bool
		enableDuplicateIncrease  bool

		additiveDuplicating bool
	}

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

		return performSync(
			cmd,
			sync.Options{
				ResolveMoves:             flags.resolveMoves,
				EnableDuplicateIncrease:  flags.enableDuplicateIncrease,
				EnableDuplicateReduction: flags.enableDuplicateReduction,

				AdditiveDuplicating: flags.additiveDuplicating,
			},
			"current", "remote", "push",
			currentArchive, remoteArchive,
		)
	}

	cmd := &cobra.Command{
		Use:   "push [other archive location]",
		Short: "Pushes changes from current archive to remote archive",
		Args:  cobra.ExactArgs(1),
		RunE:  cmdFunc,
	}

	connectionFlags.register(cmd)

	cmd.Flags().BoolVar(&flags.resolveMoves, "resolve-moves", false, "resolves added files against checksums of existing remote files")
	cmd.Flags().BoolVar(&flags.enableDuplicateIncrease, "allow-duplicate-increase", false, "allow to increase duplication of existing files")
	cmd.Flags().BoolVar(&flags.enableDuplicateReduction, "allow-duplicate-reduction", false, "allow to decrease duplication of existing files")

	cmd.Flags().BoolVar(&flags.additiveDuplicating, "additive-duplicating", false, "push all new local files to remote without resolving moves or checking for duplication increase")

	return cmd
}
