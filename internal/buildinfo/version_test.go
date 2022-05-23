package buildinfo

import "testing"

func TestVersionJson_String(t *testing.T) {
	tests := []struct {
		name    string
		version Version
		want    string
	}{
		{
			"without-tag",
			Version{
				Tag:        "",
				TagCommit:  "e79d5ef",
				Commit:     "e79d5ef",
				CommitDate: "20220522",
				Changes:    "",
			},
			"notag-e79d5ef-20220522",
		},
		{
			"clean-1.0",
			Version{
				Tag:        "v1.0",
				TagCommit:  "e79d5ef",
				Commit:     "e79d5ef",
				CommitDate: "20220522",
				Changes:    "",
			},
			"1.0-e79d5ef-20220522",
		},
		{
			"1.0-next",
			Version{
				Tag:        "v1.0",
				TagCommit:  "e79d5ef",
				Commit:     "1a2b3c4",
				CommitDate: "20220523",
				Changes:    "",
			},
			"1.0-next-1a2b3c4-20220523",
		},
		{
			"1.0-dirty",
			Version{
				Tag:        "v1.0",
				TagCommit:  "e79d5ef",
				Commit:     "e79d5ef",
				CommitDate: "20220522",
				Changes:    "A  bin/generate-version.sh",
			},
			"1.0-e79d5ef-20220522-dirty",
		},
		{
			"1.0-next-dirty",
			Version{
				Tag:        "v1.0",
				TagCommit:  "e79d5ef",
				Commit:     "1a2b3c4",
				CommitDate: "20220523",
				Changes:    "A  bin/generate-version.sh",
			},
			"1.0-next-1a2b3c4-20220523-dirty",
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
