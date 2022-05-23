package cmd

import (
	"github.com/archivekeep/archivekeep/internal/buildinfo"
	"github.com/spf13/cobra"
)

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print version information",
	RunE: func(cmd *cobra.Command, args []string) error {
		version, err := buildinfo.GetVersion()
		if err != nil {
			cmd.PrintErrf("Couldn't retrieve version. Software wasn't built properly.\n")
			return err
		}

		cmd.Printf("Version: %s\n", version.String())

		return nil
	},
}
