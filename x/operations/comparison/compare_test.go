package comparison_test

import (
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
	"github.com/archivekeep/archivekeep/x/operations/comparison"
)

func TestExecute(t *testing.T) {
	latestVersion, _ := testarchive.CreateWithContents(t, map[string]string{
		"file to be extra in source":    "file to be extra in source",
		"file to duplicate":             "file to duplicate: old",
		"file to duplicate 02":          "file to duplicate: old",
		"file to modify with backup":    "file to modify with backup: new",
		"file to move and duplicate/01": "file to move and duplicate: old",
		"file to move and duplicate/02": "file to move and duplicate: old",
		"file to overwrite":             "file to overwrite: new",
		"file to be left untouched":     "file to be left untouched: untouched",
		"moved/file to move":            "file to move: old",
		"old/file to modify backup":     "file to modify with backup: old",
	})
	outdatedArchive, _ := testarchive.CreateWithContents(t, map[string]string{
		"file to be extra in target": "file to be extra in target",
		"file to duplicate":          "file to duplicate: old",
		"file to move":               "file to move: old",
		"file to move and duplicate": "file to move and duplicate: old",
		"file to modify with backup": "file to modify with backup: old",
		"file to overwrite":          "file to overwrite: old",
		"file to be left untouched":  "file to be left untouched: untouched",
	})

	result, err := comparison.Execute(latestVersion, outdatedArchive)
	assert.NilError(t, err)
	assert.DeepEqual(t, result.Relocations, []comparison.Relocation{
		{
			OriginalFileNames:      []string{"file to duplicate"},
			NewFileNames:           []string{"file to duplicate", "file to duplicate 02"},
			ExtraOriginalFileNames: nil,
			MissingNewFileNames:    []string{"file to duplicate 02"},
		},
		{
			OriginalFileNames:      []string{"file to modify with backup"},
			NewFileNames:           []string{"old/file to modify backup"},
			ExtraOriginalFileNames: []string{"file to modify with backup"},
			MissingNewFileNames:    []string{"old/file to modify backup"},
		},
		{
			OriginalFileNames:      []string{"file to move"},
			NewFileNames:           []string{"moved/file to move"},
			ExtraOriginalFileNames: []string{"file to move"},
			MissingNewFileNames:    []string{"moved/file to move"},
		},
		{
			OriginalFileNames:      []string{"file to move and duplicate"},
			NewFileNames:           []string{"file to move and duplicate/01", "file to move and duplicate/02"},
			ExtraOriginalFileNames: []string{"file to move and duplicate"},
			MissingNewFileNames:    []string{"file to move and duplicate/01", "file to move and duplicate/02"},
		},
	})
	assert.DeepEqual(t, result.NewContentAfterMove, []string{
		"file to modify with backup",
	})
	assert.DeepEqual(t, result.NewContentToOverwrite, []string{
		"file to overwrite",
	})
	assert.DeepEqual(t, result.UnmatchedBaseExtras, []comparison.ExtraGroup{
		{
			Checksum: "1edc8804c33fd71860f80dd0f5974f09c2cf9be162ae1dc8db0bfcb820e48cef",
			Filenames: []string{
				"file to be extra in source",
			},
		},
		{
			Checksum: "64ec974de61170bc2ba1041cde9a3a9fbe23ffbc4e5bdd8db8d149c2700c0b87",
			Filenames: []string{
				"file to modify with backup",
			},
		},
		{
			Checksum: "43cd4f8a83b9a4c06ffa4c62bf4d58dc3f48633faf4c2a15cb5ee897e54b6fb6",
			Filenames: []string{
				"file to overwrite",
			},
		},
	})
	assert.DeepEqual(t, result.UnmatchedOtherExtras, []comparison.ExtraGroup{
		{
			Checksum: "c9dd58abb6a3bd6bfaf0b42d8e47b215002fbed488b53b165edfac151ff46d27",
			Filenames: []string{
				"file to be extra in target",
			},
		},
		{
			Checksum: "3e0db7aee818f73f48744520499f2e258351c9d8cef6da077cb722aff1fa8458",
			Filenames: []string{
				"file to overwrite",
			},
		},
	})
}
