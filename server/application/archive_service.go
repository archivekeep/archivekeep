package application

import (
	"context"
	"fmt"
	"strconv"
)

type ArchiveService struct {
	archiveRepository *sqlArchiveRepository
	contentStorage    *archiveContentStorage
}

func (service *ArchiveService) CreateArchive(ctx context.Context, owner string, name string) (dbArchive, error) {
	newInstance, err := service.archiveRepository.create(dbArchive{
		Owner: owner,
		Name:  name,
	})
	if err != nil {
		return dbArchive{}, fmt.Errorf("store new archive entity: %w", err)
	}

	err = service.contentStorage.create(strconv.FormatInt(newInstance.ID, 10))
	if err != nil {
		return dbArchive{}, fmt.Errorf("create storage: %w", err)
	}

	return newInstance, nil
}
