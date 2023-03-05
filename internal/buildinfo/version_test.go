package buildinfo

import (
	"testing"
	"time"
)

func TestVersionJson_String(t *testing.T) {
	tests := []struct {
		name    string
		version Version
		want    string
	}{
		{
			"without-tag",
			Version{
				Version:    "(devel)",
				VCS:        "git",
				Commit:     "e79d5ef",
				CommitDate: time.Date(2022, 5, 22, 0, 0, 0, 1, time.UTC),
				Modified:   false,
			},
			"(devel)-git-e79d5ef-20220522",
		},
		{
			"clean-1.0",
			Version{
				Version:    "v1.0",
				VCS:        "git",
				Commit:     "e79d5ef",
				CommitDate: time.Date(2022, 5, 23, 0, 0, 0, 1, time.UTC),
				Modified:   false,
			},
			"1.0-git-e79d5ef-20220523",
		},
		{
			"clean-1.0-without-info",
			Version{
				Version: "v1.0",
			},
			"1.0",
		},
		{
			"1.0-dirty",
			Version{
				Version:    "v1.0",
				VCS:        "git",
				Commit:     "e79d5ef",
				CommitDate: time.Date(2022, 5, 24, 0, 0, 0, 1, time.UTC),
				Modified:   true,
			},
			"1.0-git-e79d5ef-20220524-modified",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.version.String(); got != tt.want {
				t.Errorf("String() = %v, want %v", got, tt.want)
			}
		})
	}
}
