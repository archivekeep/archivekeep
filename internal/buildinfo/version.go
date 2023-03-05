package buildinfo

import (
	"strings"
	"time"
)

type Version struct {
	Version string

	VCS        string
	Commit     string
	CommitDate time.Time

	Modified bool
}

func (v Version) String() string {
	var result = strings.Builder{}

	if v.Version != "" {
		result.WriteString(strings.TrimLeft(v.Version, "v"))
	} else {
		result.WriteString("unknown")
	}

	if v.VCS != "" {
		result.WriteString("-")
		result.WriteString(strings.TrimSpace(v.VCS))
	}

	if v.Commit != "" {
		result.WriteString("-")
		result.WriteString(strings.TrimSpace(v.Commit))
	}

	if v.CommitDate != (time.Time{}) {
		result.WriteString("-")
		result.WriteString(strings.TrimSpace(v.CommitDate.Format("20060102")))
	}

	if v.Modified {
		result.WriteString("-modified")
	}

	return result.String()
}
