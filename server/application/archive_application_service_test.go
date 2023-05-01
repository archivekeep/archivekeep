package application

import (
	"bytes"
	"context"
	"database/sql"
	"fmt"
	paths "path"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"

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
	tapp := createTestApplication(t)
	tis := createTestAppInitialState(tapp)

	service := &archiveApplicationService{
		archiveRepository:           tapp.archiveRepository,
		archivePermissionRepository: tapp.archivePermissionRepository,
		contentStorage:              tapp.contentStorage,
	}

	tryToListArchives := func(user *User) (api.ListArchivesResult, error) {
		return service.ListArchives(
			NewContextWithAuthenticatedUser(context.Background(), user),
			api.ListArchivesRequest{},
		)
	}

	t.Run("can list own and shared archives", func(t *testing.T) {
		expectAccessList := func(user *User, a ...dbArchive) {
			t.Helper()
			result, err := tryToListArchives(user)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.Archives, toDetailsList(a))
		}

		expectAccessList(tis.user01, tis.a01, tis.a02, tis.a03, tis.a06)
		expectAccessList(tis.user02, tis.a04, tis.a06)
		expectAccessList(tis.user03, tis.a05, tis.a06)
	})
}

func Test_archiveApplicationService_GetArchive(t *testing.T) {
	tapp := createTestApplication(t)
	tis := createTestAppInitialState(tapp)

	service := &archiveApplicationService{
		archiveRepository:           tapp.archiveRepository,
		archivePermissionRepository: tapp.archivePermissionRepository,
		contentStorage:              tapp.contentStorage,
	}

	tryGetArchive := func(user *User, a dbArchive) (api.ArchiveDetails, error) {
		return service.GetArchive(NewContextWithAuthenticatedUser(context.Background(), user), a.StringID())
	}

	tryGetArchiveWithoutLogin := func(a dbArchive) (api.ArchiveDetails, error) {
		return service.GetArchive(context.Background(), a.StringID())
	}

	expectAccessOK := func(t *testing.T, user *User, a dbArchive) {
		t.Helper()
		result, err := tryGetArchive(user, a)
		assert.NilError(t, err)
		assert.DeepEqual(t, toApiArchiveDetails(a), result)
	}

	t.Run("can access own archive", func(t *testing.T) {
		expectAccessOK(t, tis.user01, tis.a01)
		expectAccessOK(t, tis.user01, tis.a02)
		expectAccessOK(t, tis.user01, tis.a03)

		expectAccessOK(t, tis.user02, tis.a04)

		expectAccessOK(t, tis.user03, tis.a05)
	})

	t.Run("can access shared archive", func(t *testing.T) {
		expectAccessOK(t, tis.user01, tis.a06)
		expectAccessOK(t, tis.user02, tis.a06)
		expectAccessOK(t, tis.user03, tis.a06)
	})

	t.Run("can't access other archive", func(t *testing.T) {
		expectAccessError := func(user *User, a dbArchive) {
			t.Helper()
			result, err := tryGetArchive(user, a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, api.ArchiveDetails{})
		}

		expectAccessError(tis.user01, tis.a04)
		expectAccessError(tis.user01, tis.a05)

		expectAccessError(tis.user02, tis.a01)
		expectAccessError(tis.user02, tis.a02)
		expectAccessError(tis.user02, tis.a03)
		expectAccessError(tis.user02, tis.a05)

		expectAccessError(tis.user03, tis.a01)
		expectAccessError(tis.user03, tis.a02)
		expectAccessError(tis.user03, tis.a03)
		expectAccessError(tis.user03, tis.a04)
	})

	t.Run("can't access without login archive", func(t *testing.T) {
		expectAccessError := func(a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveWithoutLogin(a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, api.ArchiveDetails{})
		}

		expectAccessError(tis.a01)
		expectAccessError(tis.a02)
		expectAccessError(tis.a03)
		expectAccessError(tis.a04)
		expectAccessError(tis.a05)
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
	tapp := createTestApplication(t)
	tis := createTestAppInitialState(tapp)

	service := &archiveApplicationService{
		archiveRepository:           tapp.archiveRepository,
		archivePermissionRepository: tapp.archivePermissionRepository,
		contentStorage:              tapp.contentStorage,
	}

	tryGetArchiveAccessor := func(user *User, a dbArchive) (api.ArchiveAccessor, error) {
		return service.GetArchiveAccessor(NewContextWithAuthenticatedUser(context.Background(), user), a.StringID())
	}

	t.Run("can access own archive", func(t *testing.T) {
		expectAccessOK := func(user *User, a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveAccessor(user, a)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.(ownedArchiveAccessor).id, a.StringID())
		}

		expectAccessOK(tis.user01, tis.a01)
		expectAccessOK(tis.user01, tis.a02)
		expectAccessOK(tis.user01, tis.a03)
		expectAccessOK(tis.user01, tis.a06)
		expectAccessOK(tis.user01, tis.a07)

		expectAccessOK(tis.user02, tis.a04)

		expectAccessOK(tis.user03, tis.a05)
	})

	t.Run("can access shared archive", func(t *testing.T) {
		expectReadOnlyAccessOK := func(user *User, a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveAccessor(user, a)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.(readonlyArchiveAccessor).id, a.StringID())

			reader, err := result.OpenReader()
			assert.NilError(t, err)

			_, castableToWriter := reader.(archive.Writer)
			assert.Equal(t, false, castableToWriter)

			_, err = result.OpenWriter()
			assert.Error(t, err, "not authorized")

			_, err = result.OpenReadWriter()
			assert.Error(t, err, "not authorized")
		}

		expectReadOnlyAccessOK(tis.user02, tis.a06)
		expectReadOnlyAccessOK(tis.user02, tis.a07)

		expectReadOnlyAccessOK(tis.user03, tis.a06)
	})

	t.Run("can't access other archive", func(t *testing.T) {
		expectAccessError := func(user *User, a dbArchive) {
			t.Helper()
			result, err := tryGetArchiveAccessor(user, a)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, nil)
		}

		expectAccessError(tis.user01, tis.a04)
		expectAccessError(tis.user01, tis.a05)

		expectAccessError(tis.user02, tis.a01)
		expectAccessError(tis.user02, tis.a02)
		expectAccessError(tis.user02, tis.a03)
		expectAccessError(tis.user02, tis.a05)

		expectAccessError(tis.user03, tis.a01)
		expectAccessError(tis.user03, tis.a02)
		expectAccessError(tis.user03, tis.a03)
		expectAccessError(tis.user03, tis.a04)
		expectAccessError(tis.user03, tis.a07)
	})
}

type testingApplication struct {
	archiveRepository           *sqlArchiveRepository
	archivePermissionRepository *sqlArchivePermissionRepository
	contentStorage              *archiveContentStorage
	archiveService              *ArchiveService
	userRepository              *UserRepository
	userService                 *UserService

	mustCreateArchive           func(ownerUser *User, name string) dbArchive
	mustCreateArchivePermission func(archiveID int64, user *User) dbArchivePermission
	mustCreateUser              func(userEmail string) *User
	mustStoreFile               func(archiveID int64, filename string, contents []byte)
}

func createTestApplication(t *testing.T) *testingApplication {
	db := openTestDB(t)

	archiveRepository := &sqlArchiveRepository{db: db}
	archivePermissionRepository := &sqlArchivePermissionRepository{db: db}
	contentStorage := &archiveContentStorage{
		rootDir: paths.Join(t.TempDir(), "content-root"),
	}
	archiveService := &ArchiveService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}
	userRepository := &UserRepository{
		db: db,
	}
	userService := &UserService{
		repository: userRepository,
	}

	mustCreateArchive := func(ownerUser *User, name string) dbArchive {
		archiveEntity, err := archiveService.CreateArchive(context.Background(), ownerUser.ResourceName(), name)
		assert.NilError(t, err)

		return archiveEntity
	}

	mustCreateArchivePermission := func(archiveID int64, user *User) dbArchivePermission {
		p, err := archivePermissionRepository.create(dbArchivePermission{
			ArchiveID:   archiveID,
			SubjectName: user.ByEmailResourceName(),
		})
		assert.NilError(t, err)

		return p
	}

	mustCreateUser := func(userEmail string) *User {
		user, err := userService.CreateUser(userEmail, "no-password")
		assert.NilError(t, err)

		return &user
	}

	mustStoreFile := func(archiveID int64, path string, content []byte) {
		arw, err := archiveService.contentStorage.get(fmt.Sprintf("%d", archiveID))
		assert.NilError(t, err)

		err = arw.SaveFile(bytes.NewReader(content), &archive.FileInfo{
			Path:   path,
			Length: int64(len(content)),
			Digest: map[string]string{
				"SHA256": "01534f5786529e78a0018a7d48ed385e6f20736523ab014ba505e91dd0fa0001",
			},
		})
		assert.NilError(t, err)
	}

	return &testingApplication{
		archiveRepository:           archiveRepository,
		archivePermissionRepository: archivePermissionRepository,
		contentStorage:              contentStorage,
		archiveService:              archiveService,
		userRepository:              userRepository,
		userService:                 userService,

		mustCreateArchive:           mustCreateArchive,
		mustCreateArchivePermission: mustCreateArchivePermission,
		mustCreateUser:              mustCreateUser,
		mustStoreFile:               mustStoreFile,
	}
}

type testAppInitialState struct {
	user01 *User
	user02 *User
	user03 *User

	a01 dbArchive
	a02 dbArchive
	a03 dbArchive
	a04 dbArchive
	a05 dbArchive
	a06 dbArchive
	a07 dbArchive

	a06Permission01User02 dbArchivePermission
	a06Permission01User03 dbArchivePermission
	a07Permission01User02 dbArchivePermission
}

func createTestAppInitialState(tapp *testingApplication) *testAppInitialState {
	user01 := tapp.mustCreateUser("test-user-01@somewhere.there")
	user02 := tapp.mustCreateUser("test-user-02@somewhere.there")
	user03 := tapp.mustCreateUser("test-user-03@somewhere.there")

	a01 := tapp.mustCreateArchive(user01, "Archive 01.")
	a02 := tapp.mustCreateArchive(user01, "Archive 02.")
	a03 := tapp.mustCreateArchive(user01, "Archive 03.")
	a04 := tapp.mustCreateArchive(user02, "Archive 04.")
	a05 := tapp.mustCreateArchive(user03, "Archive 05.")
	a06 := tapp.mustCreateArchive(user01, "Archive 06.")
	a07 := tapp.mustCreateArchive(user01, "Archive 07.")

	a06Permission01User02 := tapp.mustCreateArchivePermission(a06.ID, user02)
	a06Permission01User03 := tapp.mustCreateArchivePermission(a06.ID, user03)
	a07Permission01User02 := tapp.mustCreateArchivePermission(a07.ID, user02)

	tapp.mustStoreFile(a06.ID, "test-file.txt", []byte("Test File Contents"))

	return &testAppInitialState{
		user01: user01,
		user02: user02,
		user03: user03,

		a01: a01,
		a02: a02,
		a03: a03,
		a04: a04,
		a05: a05,
		a06: a06,
		a07: a07,

		a06Permission01User02: a06Permission01User02,
		a06Permission01User03: a06Permission01User03,
		a07Permission01User02: a07Permission01User02,
	}
}
