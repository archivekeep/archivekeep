package api

import (
	"github.com/archivekeep/archivekeep/archive"
)

type ArchiveAccessor interface {
	OpenReader() (archive.Reader, error)
	OpenWriter() (archive.Writer, error)

	OpenReadWriter() (archive.ReadWriter, error)

	mustEmbedUnimplementedArchiveAccessor()
}

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

func (_ UnimplementedArchiveAccessor) mustEmbedUnimplementedArchiveAccessor() {
}
