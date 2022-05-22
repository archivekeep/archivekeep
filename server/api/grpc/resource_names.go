package grpc_ak

import (
	"fmt"
	"strings"
)

type ArchiveResourceName string

func (name ArchiveResourceName) DeconstructParts() (archiveName string, err error) {
	parts := strings.Split(string(name), "/")

	if len(parts) != 2 || parts[0] != "archives" {
		return "", fmt.Errorf("invalid resource name")
	}

	return parts[1], nil
}

type ArchiveFileResourceName string

func (name ArchiveFileResourceName) DeconstructParts() (archiveName string, filePath string, err error) {
	parts := strings.SplitN(string(name), "/", 4)

	if len(parts) != 4 || parts[0] != "archives" || parts[2] != "files" {
		return "", "", fmt.Errorf("invalid resource name")
	}

	return parts[1], parts[3], nil
}
