package application

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"github.com/samber/lo"

	"github.com/archivekeep/archivekeep/server/api"
)

type archivePermissionApplicationService struct {
	api.UnimplementedArchivePermissionService

	archiveRepository           *sqlArchiveRepository
	archivePermissionRepository *sqlArchivePermissionRepository
}

func (service *archivePermissionApplicationService) ListArchivePermissions(
	ctx context.Context,
	request api.ListArchivePermissionsRequest,
) (api.ListArchivePermissionsResult, error) {
	userID, _, loggedIn := getLoggedInSubject(ctx)
	if !loggedIn {
		return api.ListArchivePermissionsResult{}, api.ErrNotAuthorized
	}

	archiveEntity, err := service.archiveRepository.get(request.ArchiveID)
	if errors.Is(err, errDbNotExist) {
		return api.ListArchivePermissionsResult{}, api.ErrNotFound
	}
	if err != nil {
		return api.ListArchivePermissionsResult{}, fmt.Errorf("retrieve archive entity: %w", err)
	}

	if archiveEntity.Owner != fmt.Sprintf("users/%d", userID) {
		return api.ListArchivePermissionsResult{}, api.ErrNotAuthorized
	}

	permissions, err := service.archivePermissionRepository.findForArchive(request.ArchiveID)
	if err != nil {
		return api.ListArchivePermissionsResult{}, fmt.Errorf("fetch permissions: %w", err)
	}

	return api.ListArchivePermissionsResult{
		ArchivePermissions: lo.Map(permissions, func(item dbArchivePermission, _ int) api.ArchivePermissionsDetails {
			return item.toAPIDTO()
		}),
	}, nil
}

func (service *archivePermissionApplicationService) CreateArchivePermission(
	ctx context.Context,
	request api.CreateArchivePermissionRequest,
) (api.ArchivePermissionsDetails, error) {
	dbArchive, err := service.getArchiveWithModificationAuthorizationCheck(ctx, request.ArchiveID)
	if err != nil {
		return api.ArchivePermissionsDetails{}, fmt.Errorf("get authorized archive: %w", err)
	}

	archivePermission := dbArchivePermission{
		ArchiveID:   dbArchive.ID,
		SubjectName: request.SubjectName,
	}

	if !strings.HasPrefix(archivePermission.SubjectName, "users-by-email/") {
		return api.ArchivePermissionsDetails{}, fmt.Errorf("subject resource isn't supported: %s", archivePermission.SubjectName)
	}

	archivePermission, err = service.archivePermissionRepository.create(archivePermission)
	if err != nil {
		return api.ArchivePermissionsDetails{}, fmt.Errorf("create permission: %w", err)
	}

	return archivePermission.toAPIDTO(), nil
}

func (service *archivePermissionApplicationService) DeletePermission(
	ctx context.Context,
	request api.DeleteArchivePermissionRequest,
) error {
	archiveEntity, err := service.getArchiveWithModificationAuthorizationCheck(ctx, request.ArchiveID)
	if err != nil {
		return fmt.Errorf("get authorized archive: %w", err)
	}

	archivePermission, err := service.archivePermissionRepository.get(request.PermissionID)
	if errors.Is(err, errDbNotExist) || archivePermission.ArchiveID != archiveEntity.ID {
		return api.ErrNotFound
	}
	if err != nil {
		return fmt.Errorf("retrieve archive permission entity: %w", err)
	}

	err = service.archivePermissionRepository.delete(archivePermission.ID)
	if err != nil {
		return fmt.Errorf("execute delete from DB: %w", err)
	}

	return nil
}

func (service *archivePermissionApplicationService) getArchiveWithModificationAuthorizationCheck(ctx context.Context, aid string) (dbArchive, error) {
	userID, _, loggedIn := getLoggedInSubject(ctx)
	if !loggedIn {
		return dbArchive{}, api.ErrNotAuthorized
	}

	archiveEntity, err := service.archiveRepository.get(aid)
	if errors.Is(err, errDbNotExist) {
		return dbArchive{}, api.ErrNotFound
	}
	if err != nil {
		return dbArchive{}, fmt.Errorf("retrieve archive entity: %w", err)
	}

	if archiveEntity.Owner != fmt.Sprintf("users/%d", userID) {
		return dbArchive{}, api.ErrNotAuthorized
	}

	return archiveEntity, nil
}
