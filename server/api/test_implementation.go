package api

import (
	"context"

	"github.com/archivekeep/archivekeep/archive"
)

type TestSingleArchiveService struct {
	UnimplementedArchiveService

	A TestArchive
}

func (t *TestSingleArchiveService) ListArchives(ctx context.Context, request ListArchivesRequest) (ListArchivesResult, error) {
	return ListArchivesResult{
		Archives: []ArchiveDetails{
			{
				ID:          t.A.Id,
				ArchiveName: t.A.DisplayName,
			},
		},
	}, nil
}

func (t *TestSingleArchiveService) GetArchiveAccessor(ctx context.Context, id string) (ArchiveAccessor, error) {
	if t.A.Id == id {
		return t.A, nil
	}

	return nil, nil
}

type TestArchive struct {
	UnimplementedArchiveAccessor

	Id          string
	DisplayName string
	A           archive.ReadWriter
}

func (t TestArchive) OpenReader() (archive.Reader, error) {
	return t.A, nil
}

func (t TestArchive) OpenWriter() (archive.Writer, error) {
	return t.A, nil
}

func (t TestArchive) OpenReadWriter() (archive.ReadWriter, error) {
	return t.A, nil
}
