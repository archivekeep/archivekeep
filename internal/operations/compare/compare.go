package compare

import (
	"fmt"
	"sort"
	"strings"

	"github.com/archivekeep/archivekeep/archive"
)

type Relocation struct {
	OriginalFileNames []string
	NewFileNames      []string

	ExtraOriginalFileNames []string
	MissingNewFileNames    []string
}

func (r *Relocation) IsIncreasingDuplicates() bool {
	return len(r.MissingNewFileNames) > len(r.ExtraOriginalFileNames)
}

func (r *Relocation) IsDecreasingDuplicates() bool {
	return len(r.MissingNewFileNames) < len(r.ExtraOriginalFileNames)
}

func diff(a, b []string) []string {
	temp := map[string]int{}
	for _, s := range a {
		temp[s]++
	}
	for _, s := range b {
		temp[s]--
	}

	var result []string
	for s, v := range temp {
		if v > 0 {
			result = append(result, s)
		}
	}
	return result
}

func newRelocation(from, to []string) Relocation {
	if len(to) == 0 {
		panic(fmt.Errorf("relocation must have at least one destination location"))
	}

	return Relocation{
		OriginalFileNames: from,
		NewFileNames:      to,

		ExtraOriginalFileNames: diff(from, to),
		MissingNewFileNames:    diff(to, from),
	}
}

type ExtraGroup struct {
	Checksum  string
	Filenames []string
}

type Result struct {
	AllBaseFiles  []string
	AllOtherFiles []string

	Relocations []Relocation

	// Paths to be having new content, after original file in base archive is moved to a new path
	NewContentAfterMove []string

	NewContentToOverwrite []string

	UnmatchedBaseExtras  []ExtraGroup
	UnmatchedOtherExtras []ExtraGroup
}

func CompareArchives(
	base archive.Reader,
	other archive.Reader,
) (Result, error) {
	baseArchiveIndex, err := buildIndex(base)
	if err != nil {
		return Result{}, fmt.Errorf("stat sync base: %w", err)
	}
	otherArchiveIndex, err := buildIndex(other)
	if err != nil {
		return Result{}, fmt.Errorf("stat sync other: %w", err)
	}

	result := Result{
		AllBaseFiles:  baseArchiveIndex.allFiles,
		AllOtherFiles: otherArchiveIndex.allFiles,
	}

	// TODO: detect differences in same files

	for checksum, baseInstances := range baseArchiveIndex.filesByChecksum {
		otherInstances, presentInOther := otherArchiveIndex.filesByChecksum[checksum]

		if !presentInOther {
			result.UnmatchedBaseExtras = append(result.UnmatchedBaseExtras, ExtraGroup{
				Checksum:  checksum,
				Filenames: baseInstances,
			})
		} else if differs(baseInstances, otherInstances) {
			result.Relocations = append(result.Relocations, newRelocation(
				otherInstances,
				baseInstances,
			))

			//for _, sourceInstance := range baseInstances {
			//	targetChecksum, targetExists := otherArchiveIndex.fileChecksum[sourceInstance]
			//
			//	if targetExists && targetChecksum != checksum {
			//		result.NewContentAfterMove = append(result.NewContentToOverwrite, sourceInstance)
			//	}
			//}
		}
	}

	for checksum, otherInstances := range otherArchiveIndex.filesByChecksum {
		_, presentInBase := baseArchiveIndex.filesByChecksum[checksum]

		if !presentInBase {
			result.UnmatchedOtherExtras = append(result.UnmatchedOtherExtras, ExtraGroup{
				Checksum:  checksum,
				Filenames: otherInstances,
			})

			for _, otherInstance := range otherInstances {
				baseInstanceChecksum, baseInstanceExists := baseArchiveIndex.fileChecksum[otherInstance]

				if baseInstanceExists && baseInstanceChecksum != checksum {
					result.NewContentToOverwrite = append(result.NewContentToOverwrite, otherInstance)
				}
			}
		} else {
			for _, otherInstance := range otherInstances {
				baseInstanceChecksum, baseInstanceExists := baseArchiveIndex.fileChecksum[otherInstance]

				if baseInstanceExists && baseInstanceChecksum != checksum {
					result.NewContentAfterMove = append(result.NewContentAfterMove, otherInstance)
				}
			}
		}

	}

	sort.Strings(result.NewContentToOverwrite)
	sort.Strings(result.NewContentAfterMove)

	sort.Slice(result.Relocations, func(i, j int) bool {
		return strings.Compare(result.Relocations[i].OriginalFileNames[0], result.Relocations[j].OriginalFileNames[0]) < 0
	})
	sort.Slice(result.UnmatchedBaseExtras, func(i, j int) bool {
		return strings.Compare(result.UnmatchedBaseExtras[i].Filenames[0], result.UnmatchedBaseExtras[j].Filenames[0]) < 0
	})
	sort.Slice(result.UnmatchedOtherExtras, func(i, j int) bool {
		return strings.Compare(result.UnmatchedOtherExtras[i].Filenames[0], result.UnmatchedOtherExtras[j].Filenames[0]) < 0
	})

	return result, nil
}

func differs(a []string, b []string) bool {
	if len(a) != len(b) {
		return true
	}

	for i := 0; i < len(a); i++ {
		if a[i] != b[i] {
			return true
		}
	}

	return false
}

type archiveIndex struct {
	allFiles []string

	fileChecksum    map[string]string
	filesByChecksum map[string][]string
}

func buildIndex(a archive.Reader) (archiveIndex, error) {
	index, err := a.ListFiles()
	if err != nil {
		return archiveIndex{}, fmt.Errorf("get index: %w", err)
	}

	allFiles := make([]string, 0, len(index))
	fileChecksum := map[string]string{}
	filesByChecksum := map[string][]string{}
	for _, info := range index {
		checksum := info.Digest["SHA256"]

		allFiles = append(allFiles, info.Path)
		fileChecksum[info.Path] = checksum
		filesByChecksum[checksum] = append(filesByChecksum[checksum], info.Path)
	}

	return archiveIndex{
		allFiles:        allFiles,
		fileChecksum:    fileChecksum,
		filesByChecksum: filesByChecksum,
	}, nil
}
