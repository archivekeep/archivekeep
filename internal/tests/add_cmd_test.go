package tests_test

import (
	"fmt"
	paths "path"
	"strings"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/assertarchive"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func Test(t *testing.T) {
	addedContents := map[string]string{
		"a":       "file_a",
		"b":       "file_b",
		"dir/a":   "dir file a",
		"dir/b":   "dir file b",
		"other/a": "other file a",
		"other/c": "other file c",
	}

	notAddedContents := map[string]string{
		"c":       "file_c",
		"d":       "file_d",
		"e":       "file_e",
		"dir/c":   "dir file c",
		"dir/f":   "dir file f",
		"other/d": "other file d",
		"other/g": "other file g",
	}

	tests := []struct {
		subdir string
		args   []string

		missingFiles map[string]string

		stdin string

		expectedOut   string
		expectedIndex []string
	}{
		{
			subdir: "dir",
			args:   []string{"add", "."},

			expectedOut: strings.Join([]string{
				"New files to be indexed:",
				"\tc",
				"\tf",
				"",
				"added: c",
				"added: f",
				"",
			}, "\n"),
			expectedIndex: []string{
				"a",
				"b",
				"dir/a",
				"dir/b",
				"dir/c",
				"dir/f",
				"other/a",
				"other/c",
			},
		},
		{
			subdir: "dir",
			args:   []string{"add", "."},

			missingFiles: map[string]string{
				"missing/old_c":        "dir file c",
				"missing/unexisting_x": "file x",
			},

			stdin: "y\n",

			expectedOut: strings.Join([]string{
				"Missing indexed files not matched by add:",
				"\t../missing/unexisting_x",
				"",
				"New files to be indexed:",
				"\tf",
				"",
				"Files to be moved:",
				"\t../missing/old_c -> c",
				"",
				"",
				"Do want to perform move? [y/n]: ",
				"proceeding ...",
				"moved: missing/old_c -> dir/c",
				"added: f",
				"",
			}, "\n"),
			expectedIndex: []string{
				"a",
				"b",
				"dir/a",
				"dir/b",
				"dir/c",
				"dir/f",
				"missing/unexisting_x",
				"other/a",
				"other/c",
			},
		},
		{
			subdir: "dir",
			args:   []string{"add", "../other"},

			expectedOut: strings.Join([]string{
				"New files to be indexed:",
				"\t../other/d",
				"\t../other/g",
				"",
				"added: ../other/d",
				"added: ../other/g",
				"",
			}, "\n"),
			expectedIndex: []string{
				"a",
				"b",
				"dir/a",
				"dir/b",
				"other/a",
				"other/c",
				"other/d",
				"other/g",
			},
		},
		{
			subdir: "dir",
			args:   []string{"add", "../dir/f", "../e", "../other/g"},

			expectedOut: strings.Join([]string{
				"New files to be indexed:",
				"\tf",
				"\t../e",
				"\t../other/g",
				"",
				"added: f",
				"added: ../e",
				"added: ../other/g",
				"",
			}, "\n"),
			expectedIndex: []string{
				"a",
				"b",
				"e",
				"dir/a",
				"dir/b",
				"dir/f",
				"other/a",
				"other/c",
				"other/g",
			},
		},
	}

	for idx, test := range tests {
		t.Run(fmt.Sprintf("%d", idx), func(t *testing.T) {
			a, archiveDir := testarchive.CreateWithContents(t, addedContents)
			createArchiveFiles(t, archiveDir, notAddedContents)
			createMissingFiles(t, archiveDir, a, test.missingFiles)

			out := runCmd(t, paths.Join(archiveDir, test.subdir), strings.NewReader(test.stdin), test.args...)
			assert.Equal(t, out, test.expectedOut)

			assertarchive.IndexContains(t, archiveDir, test.expectedIndex)
		})
	}
}
