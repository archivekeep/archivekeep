package api

import (
	"context"
)

type UnimplementedArchiveService struct {
}

func (_ UnimplementedArchiveService) ListArchives(_ context.Context, _ ListArchivesRequest) (ListArchivesResult, error) {
	return ListArchivesResult{}, ErrNotImplemented
}

func (_ UnimplementedArchiveService) GetArchive(_ context.Context, _ string) (ArchiveDetails, error) {
	return ArchiveDetails{}, ErrNotImplemented
}

func (_ UnimplementedArchiveService) GetArchiveAccessor(_ context.Context, _ string) (ArchiveAccessor, error) {
	return nil, ErrNotImplemented
}

func (_ UnimplementedArchiveService) mustEmbedUnimplementedArchiveService() {
}

type UnimplementedArchivePermissionService struct{}

func (u UnimplementedArchivePermissionService) ListArchivePermissions(ctx context.Context, request ListArchivePermissionsRequest) (ListArchivePermissionsResult, error) {
	return ListArchivePermissionsResult{}, ErrNotImplemented
}

func (u UnimplementedArchivePermissionService) CreateArchivePermission(ctx context.Context, request CreateArchivePermissionRequest) (ArchivePermissionsDetails, error) {
	return ArchivePermissionsDetails{}, ErrNotImplemented
}

func (u UnimplementedArchivePermissionService) DeletePermission(ctx context.Context, request DeleteArchivePermissionRequest) error {
	return ErrNotImplemented
}

var (
	_ ArchiveService           = UnimplementedArchiveService{}
	_ ArchivePermissionService = UnimplementedArchivePermissionService{}
)
