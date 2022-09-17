package application

import (
	"context"
	"database/sql"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/server/api"
)

func openTestDB(t testing.TB) *sql.DB {
	db, err := openOrCreateDB(paths.Join(t.TempDir(), "test.db"))
	assert.NilError(t, err)

	t.Cleanup(func() {
		db.Close()
	})

	return db
}

func Test_archiveApplicationService_ListArchives(t *testing.T) {
	db := openTestDB(t)

	// TODO: use server creator
	archiveRepository := &sqlArchiveRepository{db: db}
	contentStorage := &archiveContentStorage{
		rootDir: paths.Join(t.TempDir(), "content-root"),
	}
	archiveService := &ArchiveService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	mustCreateArchive := func(owner, name string) dbArchive {
		archiveEntity, err := archiveService.CreateArchive(context.Background(), owner, name)
		assert.NilError(t, err)

		return archiveEntity
	}

	a01 := mustCreateArchive("users/1", "Archive 01.")
	a02 := mustCreateArchive("users/1", "Archive 02.")
	a03 := mustCreateArchive("users/1", "Archive 03.")
	a04 := mustCreateArchive("users/2", "Archive 04.")
	a05 := mustCreateArchive("users/3", "Archive 05.")

	service := &archiveApplicationService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	tryToListArchives := func(userID int64) (api.ListArchivesResult, error) {
		return service.ListArchives(UserIDContext(context.Background(), userID), api.ListArchivesRequest{})
	}

	t.Run("can list own archives", func(t *testing.T) {
		expectAccessList := func(userID int64, a ...dbArchive) {
			t.Helper()
			result, err := tryToListArchives(userID)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.Archives, toDetailsList(a))
		}

		expectAccessList(1, a01, a02, a03)
		expectAccessList(2, a04)
		expectAccessList(3, a05)
	})
}

func Test_archiveApplicationService_GetArchive(t *testing.T) {
	db := openTestDB(t)

	archiveRepository := &sqlArchiveRepository{db: db}
	contentStorage := &archiveContentStorage{
		rootDir: paths.Join(t.TempDir(), "content-root"),
	}
	archiveService := &ArchiveService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	mustCreateArchive := func(owner, name string) dbArchive {
		archiveEntity, err := archiveService.CreateArchive(context.Background(), owner, name)
		assert.NilError(t, err)

		return archiveEntity
	}

	a01 := mustCreateArchive("users/1", "Archive 01.")
	a02 := mustCreateArchive("users/1", "Archive 02.")
	a03 := mustCreateArchive("users/1", "Archive 03.")
	a04 := mustCreateArchive("users/2", "Archive 04.")
	a05 := mustCreateArchive("users/3", "Archive 05.")

	service := &archiveApplicationService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	tryGetArchive := func(userID int64, a dbArchive) (api.ArchiveDetails, error) {
		return service.GetArchive(UserIDContext(context.Background(), userID), a.StringID())
	}

	tryGetArchiveWithoutLogin := func(a dbArchive) (api.ArchiveDetails, error) {
		return service.GetArchive(context.Background(), a.StringID())
	}

	t.Run("can access own archive", func(t *testing.T) {
		expectAccessOK := func(userID int64, a dbArchive) {
			t.Helper()
			result, err := tryGetArchive(userID, a)
			assert.NilError(t, err)
			assert.DeepEqual(t, toApiArchiveDetails(a), result)
		}

		expectAccessOK(1, a01)
		expectAccessOK(1, a02)
		expectAccessOK(1, a03)

		expectAccessOK(2, a04)

		expectAccessOK(3, a05)
	})

	t.Run("can't access other archive", func(t *testing.T) {
		expectAccessError := func(userID int64, a dbArchive) {
			t.Helper()
			result, err := tryGetArchive(userID, a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, api.ArchiveDetails{})
		}

		expectAccessError(1, a04)
		expectAccessError(1, a05)

		expectAccessError(2, a01)
		expectAccessError(2, a02)
		expectAccessError(2, a03)
		expectAccessError(2, a05)

		expectAccessError(3, a01)
		expectAccessError(3, a02)
		expectAccessError(3, a03)
		expectAccessError(3, a04)
	})

	t.Run("can't access without login archive", func(t *testing.T) {
		expectAccessError := func(a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveWithoutLogin(a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, api.ArchiveDetails{})
		}

		expectAccessError(a01)
		expectAccessError(a02)
		expectAccessError(a03)
		expectAccessError(a04)
		expectAccessError(a05)
	})
}

func toDetailsList(a []dbArchive) []api.ArchiveDetails {
	var am []api.ArchiveDetails
	for _, aa := range a {
		am = append(am, toApiArchiveDetails(aa))
	}
	return am
}

func Test_archiveApplicationService_GetArchiveAccessor(t *testing.T) {
	db := openTestDB(t)

	archiveRepository := &sqlArchiveRepository{db: db}
	contentStorage := &archiveContentStorage{
		rootDir: paths.Join(t.TempDir(), "content-root"),
	}
	archiveService := &ArchiveService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	mustCreateArchive := func(owner, name string) dbArchive {
		archiveEntity, err := archiveService.CreateArchive(context.Background(), owner, name)
		assert.NilError(t, err)

		return archiveEntity
	}

	a01 := mustCreateArchive("users/1", "Archive 01.")
	a02 := mustCreateArchive("users/1", "Archive 02.")
	a03 := mustCreateArchive("users/1", "Archive 03.")
	a04 := mustCreateArchive("users/2", "Archive 04.")
	a05 := mustCreateArchive("users/3", "Archive 05.")

	service := &archiveApplicationService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	tryGetArchiveAccessor := func(userID int64, a dbArchive) (api.ArchiveAccessor, error) {
		return service.GetArchiveAccessor(UserIDContext(context.Background(), userID), a.StringID())
	}

	t.Run("can access own archive", func(t *testing.T) {
		expectAccessOK := func(userID int64, a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveAccessor(userID, a)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.(ownedArchiveAccessor).id, a.StringID())
		}

		expectAccessOK(1, a01)
		expectAccessOK(1, a02)
		expectAccessOK(1, a03)

		expectAccessOK(2, a04)

		expectAccessOK(3, a05)
	})

	t.Run("can't access other archive", func(t *testing.T) {
		expectAccessError := func(userID int64, a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveAccessor(userID, a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, nil)
		}

		expectAccessError(1, a04)
		expectAccessError(1, a05)

		expectAccessError(2, a01)
		expectAccessError(2, a02)
		expectAccessError(2, a03)
		expectAccessError(2, a05)

		expectAccessError(3, a01)
		expectAccessError(3, a02)
		expectAccessError(3, a03)
		expectAccessError(3, a04)
	})
}
