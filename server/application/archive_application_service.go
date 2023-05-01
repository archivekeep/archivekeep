package application

import (
	"context"
	"errors"
	"fmt"
	"strconv"

	"github.com/archivekeep/archivekeep/server/api"

	"github.com/samber/lo"
)

type archiveApplicationService struct {
	api.UnimplementedArchiveService

	archiveRepository           *sqlArchiveRepository
	archivePermissionRepository *sqlArchivePermissionRepository
	contentStorage              *archiveContentStorage
}

func (service *archiveApplicationService) ListArchives(
	ctx context.Context,
	request api.ListArchivesRequest,
) (api.ListArchivesResult, error) {
	userID, otherIdentities, loggedIn := getLoggedInSubject(ctx)
	if !loggedIn {
		return api.ListArchivesResult{}, api.ErrNotAuthorized
	}

	dbArchives, err := service.archiveRepository.findAccessibleBy(
		fmt.Sprintf("users/%d", userID),
		otherIdentities,
	)
	if err != nil {
		return api.ListArchivesResult{}, fmt.Errorf("find owned archives: %w", err)
	}

	var result api.ListArchivesResult

	for _, a := range dbArchives {
		result.Archives = append(result.Archives, toApiArchiveDetails(a))
	}

	return result, nil
}

func (service *archiveApplicationService) GetArchive(ctx context.Context, idStr string) (api.ArchiveDetails, error) {
	userID, otherIdentities, loggedIn := getLoggedInSubject(ctx)
	if !loggedIn {
		return api.ArchiveDetails{}, api.ErrNotAuthorized
	}

	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return api.ArchiveDetails{}, fmt.Errorf("ID parsing failed: %w", err)
	}

	archiveEntity, err := service.archiveRepository.getByIntId(id)
	if errors.Is(err, errDbNotExist) {
		return api.ArchiveDetails{}, api.ErrNotFound
	}
	if err != nil {
		return api.ArchiveDetails{}, fmt.Errorf("retrieve archive entity: %w", err)
	}

	if archiveEntity.Owner != fmt.Sprintf("users/%d", userID) {
		permissions, err := service.archivePermissionRepository.findForArchive(idStr)
		if err != nil {
			return api.ArchiveDetails{}, fmt.Errorf("fetch permissions: %w", err)
		}

		_, hasPermission := lo.Find(permissions, func(p dbArchivePermission) bool {
			return lo.Contains(otherIdentities, p.SubjectName)
		})

		if !hasPermission {
			return api.ArchiveDetails{}, api.ErrNotAuthorized
		}
	}

	return toApiArchiveDetails(archiveEntity), nil
}

func toApiArchiveDetails(a dbArchive) api.ArchiveDetails {
	return api.ArchiveDetails{
		ID:          strconv.FormatInt(a.ID, 10),
		Owner:       a.Owner,
		ArchiveName: a.Name,
	}
}

func (service *archiveApplicationService) GetArchiveAccessor(ctx context.Context, idStr string) (api.ArchiveAccessor, error) {
	userID, otherIdentities, loggedIn := getLoggedInSubject(ctx)
	if !loggedIn {
		return nil, api.ErrNotAuthorized
	}

	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return nil, fmt.Errorf("ID parsing failed: %w", err)
	}

	archiveEntity, err := service.archiveRepository.getByIntId(id)
	if errors.Is(err, errDbNotExist) {
		return nil, api.ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("retrieve archive entity: %w", err)
	}

	if archiveEntity.Owner != fmt.Sprintf("users/%d", userID) {
		permissions, err := service.archivePermissionRepository.findForArchive(idStr)
		if err != nil {
			return nil, fmt.Errorf("fetch permissions: %w", err)
		}

		_, hasPermission := lo.Find(permissions, func(p dbArchivePermission) bool {
			return lo.Contains(otherIdentities, p.SubjectName)
		})

		if !hasPermission {
			return nil, api.ErrNotAuthorized
		}
	}

	arw, err := service.contentStorage.get(fmt.Sprintf("%d", archiveEntity.ID))
	if err != nil {
		return nil, fmt.Errorf("get archive from storage: %w", err)
	}

	if archiveEntity.Owner != fmt.Sprintf("users/%d", userID) {
		return readonlyArchiveAccessor{
			id:  strconv.FormatInt(archiveEntity.ID, 10),
			arw: arw,
		}, nil
	} else {
		return ownedArchiveAccessor{
			id:  strconv.FormatInt(archiveEntity.ID, 10),
			arw: arw,
		}, nil
	}

}

func (service *archiveApplicationService) CreateArchive(ctx context.Context) error {
	// TODO: auth + execute

	return api.ErrNotImplemented
}
