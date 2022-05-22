package util

import (
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

func FileExists(path string) bool {
	_, err := os.Stat(path)
	if err == nil {
		return true
	}
	if os.IsNotExist(err) {
		return false
	}
	panic(err)
}

func IsPathIgnored(path string) bool {
	ignoredDirectories := []string{".archive", ".git", ".idea"}

	for _, ignoredDirectory := range ignoredDirectories {
		if path == ignoredDirectory || strings.HasPrefix(path, ignoredDirectory+"/") {
			return true
		}
	}

	return false
}

func FindFilesByGlobs(globs ...string) []string {
	return FindFilesByGlobsIgnore(IsPathIgnored, globs...)
}

func FindFilesByGlobsIgnore(ignoreFunc func(path string) bool, globs ...string) []string {
	matchedFiles := map[string]struct{}{}

	for _, pattern := range globs {
		foundFiles, err := filepath.Glob(pattern)
		if err != nil {
			fmt.Printf("ERROR: file: %v", err)
			continue
		}

		for _, foundFile := range foundFiles {
			err := filepath.WalkDir(foundFile, func(path string, d fs.DirEntry, err error) error {
				if err != nil {
					fmt.Printf("ERROR: file: %v", err)
					return nil
				}

				if path == "." || ignoreFunc(path) || d.IsDir() {
					return nil
				}

				matchedFiles[path] = struct{}{}
				return nil
			})
			if err != nil {
				fmt.Printf("ERROR: stating files: %v", err)
			}
		}
	}

	var allFiles []string
	for path := range matchedFiles {
		allFiles = append(allFiles, path)
	}
	sort.Strings(allFiles)

	return allFiles
}
