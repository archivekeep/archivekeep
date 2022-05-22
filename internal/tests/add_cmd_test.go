package tests_test

import (
	"fmt"
	paths "path"
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

		expectedOut   string
		expectedIndex []string
	}{
		{
			subdir: "dir",
			args:   []string{"add", "."},

			expectedOut: "added: c\nadded: f\n",
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
			args:   []string{"add", "../other"},

			expectedOut: "added: ../other/d\nadded: ../other/g\n",
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

			expectedOut: "added: f\nadded: ../e\nadded: ../other/g\n",
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
			_, archiveDir := testarchive.CreateWithContents(t, addedContents)
			createArchiveFiles(t, archiveDir, notAddedContents)

			out := runCmd(t, paths.Join(archiveDir, test.subdir), nil, test.args...)
			assert.Equal(t, out, test.expectedOut)

			assertarchive.IndexContains(t, archiveDir, test.expectedIndex)
		})
	}
}
