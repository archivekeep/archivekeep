package buildinfo

import (
	"runtime/debug"
	"time"
)

var (
	// set with: -ldflags "-X github.com/archivekeep/archivekeep/internal/buildinfo.version=0.0.1"
	version string
)

func GetVersion() (Version, error) {
	info, _ := debug.ReadBuildInfo()

	versionInfo := Version{
		Version: info.Main.Version,
	}

	if versionInfo.Version == "(devel)" && version != "" {
		versionInfo.Version = version
	}

	for _, kv := range info.Settings {
		switch kv.Key {
		case "vcs":
			versionInfo.VCS = kv.Value
		case "vcs.revision":
			versionInfo.Commit = kv.Value
		case "vcs.time":
			versionInfo.CommitDate, _ = time.Parse(time.RFC3339, kv.Value)
		case "vcs.modified":
			versionInfo.Modified = kv.Value == "true"
		}
	}

	return versionInfo, nil
}
