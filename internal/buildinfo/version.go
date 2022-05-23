package buildinfo

import (
	"strings"
)

type Version struct {
	Tag, TagCommit string

	Commit, CommitDate string

	Changes string
}

func (v Version) String() string {
	var result = strings.Builder{}

	v.Tag = strings.TrimSpace(v.Tag)
	v.TagCommit = strings.TrimSpace(v.TagCommit)
	v.Commit = strings.TrimSpace(v.Commit)
	v.CommitDate = strings.TrimSpace(v.CommitDate)
	v.Changes = strings.TrimSpace(v.Changes)

	if v.Tag != "" {
		result.WriteString(strings.TrimLeft(v.Tag, "v"))

		if v.TagCommit != v.Commit {
			result.WriteString("-next")
		}
	} else {
		result.WriteString("notag")
	}

	result.WriteString("-")
	result.WriteString(v.Commit)
	result.WriteString("-")
	result.WriteString(v.CommitDate)

	if v.Changes != "" {
		result.WriteString("-dirty")
	}

	return result.String()
}
