package api

import (
	"context"
)

type ArchivePermissionService interface {
	ListArchivePermissions(ctx context.Context, request ListArchivePermissionsRequest) (ListArchivePermissionsResult, error)

	CreateArchivePermission(ctx context.Context, request CreateArchivePermissionRequest) (ArchivePermissionsDetails, error)

	DeletePermission(ctx context.Context, request DeleteArchivePermissionRequest) error
}

type ArchivePermissionsDetails struct {
	ArchiveID    string
	PermissionID string

	// SubjectName should represent resource name having access to the archive, i.e.
	// it could be users-by-email/someone@there.com
	SubjectName string

	// TODO: add support for granular permissions, or archive-local roles
	// now it's just read-only access
}

type ListArchivePermissionsRequest struct {
	ArchiveID string
}

type ListArchivePermissionsResult struct {
	ArchivePermissions []ArchivePermissionsDetails
}

type CreateArchivePermissionRequest struct {
	ArchiveID string

	// SubjectName to share archive with, see SubjectName in ArchivePermissionsDetails
	SubjectName string
}

type DeleteArchivePermissionRequest struct {
	ArchiveID    string
	PermissionID string
}
