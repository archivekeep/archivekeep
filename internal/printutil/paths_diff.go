package printutil

import (
	"fmt"
	"strings"
)

func PathDiff(
	old,
	new string,
	colorFunc func(string) string,
) string {
	oldDir, oldName := splitDirName(old)
	newDir, newName := splitDirName(new)

	dirDiffPart := ""
	if oldDir != "" || newDir != "" {
		if oldDir == newDir {
			dirDiffPart = oldDir
		} else {
			dirLCP := lcp(oldDir, newDir)
			remainingOldDir := strings.TrimPrefix(oldDir, dirLCP)
			remainingNewDir := strings.TrimPrefix(newDir, dirLCP)

			dirLCS := lcf(remainingOldDir, remainingNewDir)

			dirDiffMiddlePart := fmt.Sprintf(
				"{%s -> %s}",
				strings.TrimSuffix(remainingOldDir, dirLCS),
				strings.TrimSuffix(remainingNewDir, dirLCS),
			)

			dirDiffPart = fmt.Sprintf(
				"%s%s%s",
				dirLCP,
				colorFunc(dirDiffMiddlePart),
				dirLCS,
			)
		}
	}

	if oldName == newName {
		return fmt.Sprintf("%s%s", dirDiffPart, oldName)
	} else {
		if dirDiffPart == "" {
			return colorFunc(fmt.Sprintf("%s -> %s", oldName, newName))
		} else {
			nameDiffPart := fmt.Sprintf("{%s -> %s}", oldName, newName)

			return fmt.Sprintf("%s%s", dirDiffPart, colorFunc(nameDiffPart))
		}
	}

}

func splitDirName(path string) (string, string) {
	idx := strings.LastIndex(path, "/")

	if idx == -1 {
		return "", path
	} else {
		return path[:idx+1], path[idx+1:]
	}
}

func lcp(a string, b string) string {
	for i := 0; ; i++ {
		if i == len(a) {
			return a
		}

		if i == len(b) {
			return b
		}

		if a[i] != b[i] {
			return a[:i]
		}
	}
}

func lcf(a string, b string) string {
	for i := 0; ; i++ {
		if i == len(a) {
			return a
		}

		if i == len(b) {
			return b
		}

		if a[len(a)-i-1] != b[len(b)-i-1] {
			return a[len(a)-i:]
		}
	}
}
