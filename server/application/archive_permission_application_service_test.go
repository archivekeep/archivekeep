package application

import (
	"context"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/server/api"
)

func TestArchivePermissionApplicationService_ListArchivePermissions(t *testing.T) {
	tapp := createTestApplication(t)
	tis := createTestAppInitialState(tapp)

	service := &archivePermissionApplicationService{
		archiveRepository:           tapp.archiveRepository,
		archivePermissionRepository: tapp.archivePermissionRepository,
	}

	tryToListPermissions := func(user *User, archiveID string) (api.ListArchivePermissionsResult, error) {
		return service.ListArchivePermissions(NewContextWithAuthenticatedUser(context.Background(), user), api.ListArchivePermissionsRequest{
			ArchiveID: archiveID,
		})
	}

	t.Run("can list permissions of own archives", func(t *testing.T) {
		expectAccessList := func(user *User, archiveID string, a ...dbArchivePermission) {
			t.Helper()
			result, err := tryToListPermissions(user, archiveID)
			assert.NilError(t, err)
			assert.DeepEqual(t, result.ArchivePermissions, toPermissionDetailsList(a))
		}

		expectAccessList(tis.user01, tis.a01.StringID())
		expectAccessList(tis.user01, tis.a02.StringID())
		expectAccessList(tis.user01, tis.a03.StringID())
		expectAccessList(tis.user01, tis.a06.StringID(), tis.a06Permission01User02, tis.a06Permission01User03)
		expectAccessList(tis.user01, tis.a07.StringID(), tis.a07Permission01User02)

		expectAccessList(tis.user02, tis.a04.StringID())
		expectAccessList(tis.user03, tis.a05.StringID())
	})

	t.Run("forbidden to list permissions of non-owned archives", func(t *testing.T) {
		expectAccessListForbidden := func(user *User, archiveID string, a ...dbArchivePermission) {
			t.Helper()
			result, err := tryToListPermissions(user, archiveID)
			assert.ErrorContains(t, err, api.ErrNotAuthorized.Error())
			assert.DeepEqual(t, result, api.ListArchivePermissionsResult{})
		}

		expectAccessListForbidden(tis.user01, tis.a04.StringID())
		expectAccessListForbidden(tis.user01, tis.a05.StringID())

		expectAccessListForbidden(tis.user02, tis.a01.StringID())
		expectAccessListForbidden(tis.user02, tis.a02.StringID())
		expectAccessListForbidden(tis.user02, tis.a03.StringID())
		expectAccessListForbidden(tis.user02, tis.a05.StringID())
		expectAccessListForbidden(tis.user02, tis.a06.StringID())

		expectAccessListForbidden(tis.user03, tis.a01.StringID())
		expectAccessListForbidden(tis.user03, tis.a02.StringID())
		expectAccessListForbidden(tis.user03, tis.a03.StringID())
		expectAccessListForbidden(tis.user03, tis.a04.StringID())
		expectAccessListForbidden(tis.user03, tis.a06.StringID())
	})
}

func toPermissionDetailsList(a []dbArchivePermission) []api.ArchivePermissionsDetails {
	am := []api.ArchivePermissionsDetails{}
	for _, ap := range a {
		am = append(am, ap.toAPIDTO())
	}
	return am
}
