package tests_test

import (
	"fmt"
	"strings"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestBasicSync(t *testing.T) {
	_, archiveDirA := testarchive.Create(t)
	_, archiveDirB := testarchive.Create(t)

	createArchiveFile(t, archiveDirA, "file_a", "A")
	createArchiveFile(t, archiveDirA, "file_b", "A")
	createArchiveFile(t, archiveDirB, "file_c", "B")
	createArchiveFile(t, archiveDirB, "file_d", "B")

	out := runCmd(t, archiveDirA, nil, "add", "file_a", "file_b")
	assert.Equal(t, out, "added: file_a\nadded: file_b\n")

	out = runCmd(t, archiveDirB, nil, "add", "file_c", "file_d")
	assert.Equal(t, out, "added: file_c\nadded: file_d\n")

	out = runCmd(t, archiveDirA, nil, "compare", archiveDirB)
	assert.Equal(t, out, `
Extra local files not matched to remote files:
	file_a
	file_b

Extra local files not matched to remote files:
	file_c
	file_d

Extra files in local archive: 2
Extra files in remote archive: 2
Total files present in both archives: 0

`)

	out = runCmd(t, archiveDirA, strings.NewReader("y\n"), "push", archiveDirB)
	assert.Equal(t, out, `
Files to be pushed from current to remote archive:
	file_a
	file_b

Extra files in current archive: 2
Extra files in remote archive: 2
Total files present in both archives: 0

Do you want to perform push? [y/n]: 
proceeding ...
file copied: file_a
file copied: file_b
`)
	assertArchiveFile(t, archiveDirB, "file_a", "A")
	assertArchiveFile(t, archiveDirB, "file_b", "A")

	out = runCmd(t, archiveDirA, strings.NewReader("y\n"), "pull", archiveDirB)
	assert.Equal(t, out, `
Files to be pulled from remote to current archive:
	file_c
	file_d

Extra files in remote archive: 2
Extra files in current archive: 0
Total files present in both archives: 2

Do you want to perform pull? [y/n]: 
proceeding ...
file copied: file_c
file copied: file_d
`)
	assertArchiveFile(t, archiveDirA, "file_c", "B")
	assertArchiveFile(t, archiveDirA, "file_d", "B")

	out = runCmd(t, archiveDirA, nil, "compare", archiveDirB)
	assert.Equal(t, out, `
Extra files in local archive: 0
Extra files in remote archive: 0
Total files present in both archives: 4

`)
}

func TestPushWontOverwriteNonIndexedContents(t *testing.T) {
	_, currentArchiveDir := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
		"b": "source b",
	})

	_, remoteArchiveDir := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
	})
	createArchiveFiles(t, remoteArchiveDir, map[string]string{
		"b": "non indexed target b",
	})

	out, err := runCmdError(t, currentArchiveDir, strings.NewReader("y\n"), "push", remoteArchiveDir)
	assert.Equal(t, out, strings.Join([]string{
		"",
		"Files to be pushed from current to remote archive:",
		"\tb",
		"",
		"Extra files in current archive: 1",
		"Extra files in remote archive: 0",
		"Total files present in both archives: 1",
		"",
		"Do you want to perform push? [y/n]: ",
		"proceeding ...",
		"",
	}, "\n"))
	assert.Error(t, err, fmt.Sprintf("push new files: transfer file b: open %s/b: file exists", remoteArchiveDir))

	assertArchiveFileContains(t, remoteArchiveDir, "b", "non indexed target b")
}

func TestPullWontOverwriteNonIndexedContents(t *testing.T) {
	_, currentArchiveDir := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
	})

	createArchiveFiles(t, currentArchiveDir, map[string]string{
		"b": "non indexed original b",
	})
	_, remoteArchiveDir := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
		"b": "target b",
	})

	out, err := runCmdError(t, currentArchiveDir, strings.NewReader("y\n"), "pull", remoteArchiveDir)
	assert.Equal(t, out, strings.Join([]string{
		"",
		"Files to be pulled from remote to current archive:",
		"\tb",
		"",
		"Extra files in remote archive: 1",
		"Extra files in current archive: 0",
		"Total files present in both archives: 1",
		"",
		"Do you want to perform pull? [y/n]: ",
		"proceeding ...",
		"",
	}, "\n"))
	assert.Error(t, err, fmt.Sprintf("pull new files: transfer file b: open %s/b: file exists", currentArchiveDir))

	assertArchiveFileContains(t, currentArchiveDir, "b", "non indexed original b")
}

func TestResolveMoves(t *testing.T) {
	t.SkipNow()

	// TODO: test following scenarios:
	//  - within same directory,
	//  - to other non-existing directory in remote archive,
	//  - to existing file not-added to archive index (don't overwrite)
}
