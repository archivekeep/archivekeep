package api

import (
	"context"
	"io"

	"github.com/archivekeep/archivekeep/archive"
)

type ArchiveAccessor interface {
	OpenReader() (archive.Reader, error)
	OpenWriter() (archive.Writer, error)

	OpenReadWriter() (archive.ReadWriter, error)

	// GetThumbnail should return a thumbnail for file at given path
	// TODO: add support for image formats (JPEG, WEBP,...) and move to more appropriate place
	GetThumbnail(ctx context.Context, filePath string) (io.ReadCloser, error)

	mustEmbedUnimplementedArchiveAccessor()
}

var (
	_ ArchiveAccessor = UnimplementedArchiveAccessor{}
)

type UnimplementedArchiveAccessor struct {
}

func (_ UnimplementedArchiveAccessor) OpenReader() (archive.Reader, error) {
	return nil, ErrNotImplemented
}

func (_ UnimplementedArchiveAccessor) OpenWriter() (archive.Writer, error) {
	return nil, ErrNotImplemented
}

func (_ UnimplementedArchiveAccessor) OpenReadWriter() (archive.ReadWriter, error) {
	return nil, ErrNotImplemented
}

func (_ UnimplementedArchiveAccessor) GetThumbnail(_ context.Context, _ string) (io.ReadCloser, error) {
	return nil, ErrNotImplemented
}

func (_ UnimplementedArchiveAccessor) mustEmbedUnimplementedArchiveAccessor() {
}
