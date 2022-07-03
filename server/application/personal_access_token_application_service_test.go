package application_test

import (
	"context"
	"testing"

	"github.com/archivekeep/archivekeep/server/api"
	"github.com/archivekeep/archivekeep/server/application"
	"gotest.tools/v3/assert"
)

func Test_personalAccessTokenApplicationService_CreatePersonalAccessToken(t *testing.T) {
	server, _, closeServer := createTestServer(t)
	defer closeServer()

	var (
		testUser01Name     = "test.user@localhost"
		testUser01Password = "password-01"

		testUser02Name     = "another.test.user@localhost"
		testUser02Password = "password-02"
	)

	testUser01, err := server.UserService.CreateUser(testUser01Name, testUser01Password)
	assert.NilError(t, err)

	testUser02, err := server.UserService.CreateUser(testUser02Name, testUser02Password)
	assert.NilError(t, err)

	testUser01Token01, err := server.PersonalAccessTokenApplicationService.CreatePersonalAccessToken(
		application.UserIDContext(context.TODO(), testUser01.ID),
		&api.CreatePersonalAccessTokenRequest{Name: "the first token"},
	)
	assert.NilError(t, err)
	assert.Assert(t, testUser01Token01 != nil)

	testUser01Token02, err := server.PersonalAccessTokenApplicationService.CreatePersonalAccessToken(
		application.UserIDContext(context.TODO(), testUser01.ID),
		&api.CreatePersonalAccessTokenRequest{Name: "the second token"},
	)
	assert.NilError(t, err)
	assert.Assert(t, testUser01Token02 != nil)

	testUser02Token01, err := server.PersonalAccessTokenApplicationService.CreatePersonalAccessToken(
		application.UserIDContext(context.TODO(), testUser02.ID),
		&api.CreatePersonalAccessTokenRequest{Name: "the first token"},
	)
	assert.NilError(t, err)
	assert.Assert(t, testUser02Token01 != nil)

	{
		_, err = server.PersonalAccessTokenApplicationService.CreatePersonalAccessToken(
			application.UserIDContext(context.TODO(), testUser01.ID),
			&api.CreatePersonalAccessTokenRequest{Name: "the second token"},
		)
		assert.ErrorContains(t, err, "UNIQUE constraint failed")
	}

	{
		testUser01Token01LoginUser, err := server.PersonalAccessTokenApplicationService.TryTokenAuth(context.TODO(), testUser01Token01.Token)
		assert.NilError(t, err)
		assert.Equal(t, testUser01.ID, testUser01Token01LoginUser.ID)
	}

	{
		testUser01Token02LoginUser, err := server.PersonalAccessTokenApplicationService.TryTokenAuth(context.TODO(), testUser01Token02.Token)
		assert.NilError(t, err)
		assert.Equal(t, testUser01.ID, testUser01Token02LoginUser.ID)
	}

	{
		testUser02Token01LoginUser, err := server.PersonalAccessTokenApplicationService.TryTokenAuth(context.TODO(), testUser02Token01.Token)
		assert.NilError(t, err)
		assert.Equal(t, testUser02.ID, testUser02Token01LoginUser.ID)
	}
}
