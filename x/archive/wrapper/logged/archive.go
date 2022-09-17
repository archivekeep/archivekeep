package logged

import (
	"io"

	"github.com/kr/pretty"

	archiveapi "github.com/archivekeep/archivekeep/archive"
)

type Archive struct {
	Base archiveapi.ReadWriter

	Printfer pretty.Printfer
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

func (a *Archive) SaveFile(reader io.Reader, fileInfo *archiveapi.FileInfo) error {
	err := a.Base.SaveFile(reader, fileInfo)
	if err != nil {
		a.Printfer.Printf("error storing file '%s': %s\n", fileInfo.Path, err)
		return err
	}

	a.Printfer.Printf("file stored: %s\n", fileInfo.Path)
	return err
}

func (a *Archive) MoveFile(from string, to string) error {
	err := a.Base.MoveFile(from, to)
	if err != nil {
		a.Printfer.Printf("error moving file: %s\n", err)
		return err
	}

	a.Printfer.Printf("file moved from '%s' to '%s'\n", from, to)
	return err
}

func (a *Archive) DeleteFile(path string) error {
	err := a.Base.DeleteFile(path)
	if err != nil {
		a.Printfer.Printf("error deleting file: %s\n", path)
		return err
	}

	a.Printfer.Printf("file deleted: %s\n", path)
	return err
}
