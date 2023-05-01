package api

import (
	"context"
)

// ArchiveService is an application service exposing access to archives.
//
// Authorization MUST be performed by ArchiveService on each method call. If a method returns a rich object,
// like GetArchiveAccessor, then implementation of these objects MUST ensure correct access control policies
// are applied for the user, for which the object was returned.
type ArchiveService interface {
	ListArchives(ctx context.Context, request ListArchivesRequest) (ListArchivesResult, error)
	GetArchive(ctx context.Context, id string) (ArchiveDetails, error)

	GetArchiveAccessor(ctx context.Context, id string) (ArchiveAccessor, error)

	mustEmbedUnimplementedArchiveService()
}

type ArchiveDetails struct {
	ID    string
	Owner string

	ArchiveName string
}

type ListArchivesRequest struct{}

type ListArchivesResult struct {
	Archives []ArchiveDetails
}
