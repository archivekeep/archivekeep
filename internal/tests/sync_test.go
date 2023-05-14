package tests_test

import (
	"fmt"
	"strings"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
)

func TestBasicSync(t *testing.T) {
	archiveA := testarchive.Create(t)
	archiveB := testarchive.Create(t)

	createArchiveFile(t, archiveA.Dir, "file_a", "A")
	createArchiveFile(t, archiveA.Dir, "file_b", "A")
	createArchiveFile(t, archiveB.Dir, "file_c", "B")
	createArchiveFile(t, archiveB.Dir, "file_d", "B")

	out := runCmd(t, archiveA.Dir, nil, "add", "--do-not-print-preparation-summary", "file_a", "file_b")
	assert.Equal(t, out, "added: file_a\nadded: file_b\n")

	out = runCmd(t, archiveB.Dir, nil, "add", "--do-not-print-preparation-summary", "file_c", "file_d")
	assert.Equal(t, out, "added: file_c\nadded: file_d\n")

	out = runCmd(t, archiveA.Dir, nil, "compare", archiveB.Dir)
	assert.Equal(t, out, `
Extra files in local archive:
	file_a
	file_b

Extra files in remote archive:
	file_c
	file_d

Extra files in local archive: 2
Extra files in remote archive: 2
Total files present in both archives: 0

`)

	out = runCmd(t, archiveA.Dir, strings.NewReader("y\n"), "push", archiveB.Dir)
	assert.Equal(t, out, `
Extra files in current archive:
	file_a
	file_b

Extra files in remote archive:
	file_c
	file_d

Extra files in current archive: 2
Extra files in remote archive: 2
Total files present in both archives: 0

Do you want to perform push? [y/n]: 
file stored: file_a
file stored: file_b
`)
	assertArchiveFile(t, archiveB.Dir, "file_a", "A")
	assertArchiveFile(t, archiveB.Dir, "file_b", "A")

	out = runCmd(t, archiveA.Dir, strings.NewReader("y\n"), "pull", archiveB.Dir)
	assert.Equal(t, out, `
Extra files in remote archive:
	file_c
	file_d

Extra files in remote archive: 2
Extra files in current archive: 0
Total files present in both archives: 2

Do you want to perform pull? [y/n]: 
file stored: file_c
file stored: file_d
`)
	assertArchiveFile(t, archiveA.Dir, "file_c", "B")
	assertArchiveFile(t, archiveA.Dir, "file_d", "B")

	out = runCmd(t, archiveA.Dir, nil, "compare", archiveB.Dir)
	assert.Equal(t, out, `
Extra files in local archive: 0
Extra files in remote archive: 0
Total files present in both archives: 4

`)
}

func TestPushWontOverwriteNonIndexedContents(t *testing.T) {
	currentArchive := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
		"b": "source b",
	})

	remoteArchive := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
	})
	remoteArchive.CreateUnindexedArchiveFiles(map[string]string{
		"b": "non indexed target b",
	})

	out, err := runCmdError(t, currentArchive.Dir, strings.NewReader("y\n"), "push", remoteArchive.Dir)
	assert.Equal(t, out, terminalLines(
		"",
		"Extra files in current archive:",
		"\tb",
		"",
		"Extra files in current archive: 1",
		"Extra files in remote archive: 0",
		"Total files present in both archives: 1",
		"",
		"Do you want to perform push? [y/n]: ",
		"error storing file 'b': open "+remoteArchive.Dir+"/b: file exists",
	))
	assert.Error(t, err, fmt.Sprintf("execute push: copy new files: transfer file b: open %s/b: file exists", remoteArchive.Dir))

	assertArchiveFileContains(t, remoteArchive.Dir, "b", "non indexed target b")
}

func TestPullWontOverwriteNonIndexedContents(t *testing.T) {
	currentArchive := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
	})

	currentArchive.CreateUnindexedArchiveFiles(map[string]string{
		"b": "non indexed original b",
	})
	remoteArchive := testarchive.CreateWithContents(t, map[string]string{
		"a": "original a",
		"b": "target b",
	})

	out, err := runCmdError(t, currentArchive.Dir, strings.NewReader("y\n"), "pull", remoteArchive.Dir)
	assert.Equal(t, out, terminalLines(
		"",
		"Extra files in remote archive:",
		"\tb",
		"",
		"Extra files in remote archive: 1",
		"Extra files in current archive: 0",
		"Total files present in both archives: 1",
		"",
		"Do you want to perform pull? [y/n]: ",
		"error storing file 'b': open "+currentArchive.Dir+"/b: file exists",
	))
	assert.Error(t, err, fmt.Sprintf("execute pull: copy new files: transfer file b: open %s/b: file exists", currentArchive.Dir))

	assertArchiveFileContains(t, currentArchive.Dir, "b", "non indexed original b")
}

func TestResolveMoves(t *testing.T) {
	t.SkipNow()

	// TODO: test following scenarios:
	//  - within same directory,
	//  - to other non-existing directory in remote archive,
	//  - to existing file not-added to archive index (don't overwrite)
}
