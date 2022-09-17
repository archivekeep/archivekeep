package sharing

import (
	"context"
	"fmt"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/server/api"
)

type singleArchiveService struct {
	api.UnimplementedArchiveService
	api.UnimplementedArchiveAccessor

	name              string
	archiveReadWriter archive.ReadWriter
}

func (a *singleArchiveService) ListArchives(ctx context.Context, request api.ListArchivesRequest) (api.ListArchivesResult, error) {
	return api.ListArchivesResult{
		Archives: []api.ArchiveDetails{
			a.toApiArchiveDetails(),
		},
	}, nil
}

func (a *singleArchiveService) GetArchive(ctx context.Context, id string) (api.ArchiveDetails, error) {
	if id != a.ID() {
		return api.ArchiveDetails{}, api.ErrNotFound
	}

	return a.toApiArchiveDetails(), nil
}

func (a *singleArchiveService) toApiArchiveDetails() api.ArchiveDetails {
	return api.ArchiveDetails{
		ID:          a.ID(),
		ArchiveName: a.name,
	}
}

func (a *singleArchiveService) GetArchiveAccessor(_ context.Context, id string) (api.ArchiveAccessor, error) {
	if id == a.name {
		return a, nil
	}

	return nil, fmt.Errorf("not found")
}

func (a *singleArchiveService) ID() string {
	return a.name
}

func (a *singleArchiveService) OpenReader() (archive.Reader, error) {
	return a.archiveReadWriter, nil
}

func (a *singleArchiveService) OpenWriter() (archive.Writer, error) {
	return a.archiveReadWriter, nil
}

func (a *singleArchiveService) OpenReadWriter() (archive.ReadWriter, error) {
	return a.archiveReadWriter, nil
}
