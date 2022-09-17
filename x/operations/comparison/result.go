package comparison

import (
	"strings"

	"github.com/kr/pretty"
)

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

func (compareResult Result) PrintAll(p pretty.Printfer, baseName string, otherName string) {
	compareResult.PrintUnmatchedBaseExtras(p, baseName, otherName)
	compareResult.PrintUnmatchedOtherExtras(p, baseName, otherName)
	compareResult.PrintRelocations(p)

	// TODO: show list of same filenames with different contents and other comparison results

	compareResult.PrintStats(p, baseName, otherName)
}

func (compareResult Result) PrintUnmatchedBaseExtras(out pretty.Printfer, baseName string, _ string) {
	if len(compareResult.UnmatchedBaseExtras) > 0 {
		out.Printf("\nExtra files in %s archive:\n", baseName)
		for _, sourceExtra := range compareResult.UnmatchedBaseExtras {
			out.Printf("\t%s\n", filenamesPrint(sourceExtra.Filenames))
		}
	}
}

func (compareResult Result) PrintUnmatchedOtherExtras(out pretty.Printfer, _ string, otherName string) {
	if len(compareResult.UnmatchedOtherExtras) > 0 {
		out.Printf("\nExtra files in %s archive:\n", otherName)
		for _, targetExtra := range compareResult.UnmatchedOtherExtras {
			out.Printf("\t%s\n", filenamesPrint(targetExtra.Filenames))
		}
	}
}

func (compareResult Result) PrintRelocations(out pretty.Printfer) {
	if len(compareResult.Relocations) > 0 {
		out.Printf("\nFiles to be moved:\n")
		for _, move := range compareResult.Relocations {
			out.Printf("\t%s -> %s\n", filenamesPrint(move.OriginalFileNames), filenamesPrint(move.NewFileNames))
		}
	}
}

func (compareResult Result) PrintStats(
	out pretty.Printfer,
	baseName string,
	otherName string,
) {
	out.Printf("\n")

	out.Printf("Extra files in %s archive: %d\n", baseName, len(compareResult.UnmatchedBaseExtras))
	out.Printf("Extra files in %s archive: %d\n", otherName, len(compareResult.UnmatchedOtherExtras))
	out.Printf("Total files present in both archives: %d\n", len(compareResult.AllBaseFiles)-len(compareResult.UnmatchedBaseExtras))

	out.Printf("\n")
}

func filenamesPrint(filenames []string) string {
	if len(filenames) == 1 {
		return filenames[0]
	} else {
		return "{" + strings.Join(filenames, ", ") + "}"
	}
}
