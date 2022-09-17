package readonly

import (
	"fmt"
	"io"

	archiveapi "github.com/archivekeep/archivekeep/archive"
)

type Archive struct {
	Base archiveapi.Reader
}

func (a *Archive) ListFiles() ([]*archiveapi.FileInfo, error) {
	return a.Base.ListFiles()
}

func (a *Archive) OpenFile(filename string) (archiveapi.FileInfo, io.ReadCloser, error) {
	return a.Base.OpenFile(filename)
}

func (a *Archive) OpenSeekableFile(filename string) (archiveapi.FileInfo, io.ReadSeekCloser, error) {
	return a.Base.OpenSeekableFile(filename)
}

func (a *Archive) OpenReaderAtFile(filename string) (archiveapi.FileInfo, archiveapi.FileReadAtCloser, error) {
	return a.Base.OpenReaderAtFile(filename)
}

func (a *Archive) SaveFile(reader io.Reader, filename *archiveapi.FileInfo) error {
	return fmt.Errorf("write access is denied")
}

func (a *Archive) MoveFile(from string, to string) error {
	return fmt.Errorf("write access is denied")
}

func (a *Archive) DeleteFile(filename string) error {
	return fmt.Errorf("write access is denied")
}
