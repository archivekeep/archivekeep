package application

import (
	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/server/api"
)

type ownedArchiveAccessor struct {
	api.UnimplementedArchiveAccessor

	id  string
	arw archive.ReadWriter
}

func (a ownedArchiveAccessor) OpenReader() (archive.Reader, error) {
	return a.arw, nil
}

func (a ownedArchiveAccessor) OpenWriter() (archive.Writer, error) {
	return a.arw, nil
}

func (a ownedArchiveAccessor) OpenReadWriter() (archive.ReadWriter, error) {
	return a.arw, nil
}
