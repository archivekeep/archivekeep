package application

import (
	"context"
	"io"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/server/api"
)

type readonlyArchiveAccessor struct {
	api.UnimplementedArchiveAccessor

	id  string
	arw archive.ReadWriter
}

func (a readonlyArchiveAccessor) OpenReader() (archive.Reader, error) {
	return readOnlyProtector{ar: a.arw}, nil
}

func (a readonlyArchiveAccessor) OpenWriter() (archive.Writer, error) {
	return nil, api.ErrNotAuthorized
}

func (a readonlyArchiveAccessor) OpenReadWriter() (archive.ReadWriter, error) {
	return nil, api.ErrNotAuthorized
}

func (a readonlyArchiveAccessor) GetThumbnail(ctx context.Context, filePath string) (io.ReadCloser, error) {
	archiveReader, err := a.OpenReader()
	if err != nil {
		return nil, err
	}

	return getThumbnail(ctx, filePath, archiveReader)
}

type readOnlyProtector struct {
	ar archive.Reader
}

func (r readOnlyProtector) ListFiles() ([]*archive.FileInfo, error) {
	return r.ar.ListFiles()
}

func (r readOnlyProtector) OpenFile(filePath string) (archive.FileInfo, io.ReadCloser, error) {
	return r.ar.OpenFile(filePath)
}

func (r readOnlyProtector) OpenSeekableFile(filePath string) (archive.FileInfo, io.ReadSeekCloser, error) {
	return r.ar.OpenSeekableFile(filePath)
}

func (r readOnlyProtector) OpenReaderAtFile(filePath string) (archive.FileInfo, archive.FileReadAtCloser, error) {
	return r.ar.OpenReaderAtFile(filePath)
}
