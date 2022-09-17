package application_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/cookiejar"
	"strings"
	"testing"

	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/server/application"
)

func Test_SessionBasedAuthentication(t *testing.T) {
	server, listeners, closeServer := createTestServer(t)
	defer closeServer()

	testUserName := "test.user@localhost"
	testUserPassword := "test-password"

	_, err := server.UserService.CreateUser(
		testUserName,
		testUserPassword,
	)
	assert.NilError(t, err)

	t.Run("Login and logout", func(t *testing.T) {
		client := createTestClient(t, listeners)

		loginResponse := client.PostJSON(t, "/api/auth/session/login", map[string]interface{}{
			"Username": testUserName,
			"Password": testUserPassword,
		})
		assert.DeepEqual(t, loginResponse.StatusCode, 200)

		logoutResponse := client.PostJSON(t, "/api/auth/session/logout", map[string]interface{}{})
		assert.DeepEqual(t, logoutResponse.StatusCode, 200)
	})

	t.Run("Prevent login with incorrect username", func(t *testing.T) {
		client := createTestClient(t, listeners)

		loginResponse := client.PostJSON(t, "/api/auth/session/login", map[string]interface{}{
			"Username": "wrong username",
			"Password": testUserPassword,
		})
		assert.DeepEqual(t, loginResponse.StatusCode, 403)
	})

	t.Run("Prevent login with incorrect password", func(t *testing.T) {
		client := createTestClient(t, listeners)

		loginResponse := client.PostJSON(t, "/api/auth/session/login", map[string]interface{}{
			"Username": testUserName,
			"Password": "wrong password",
		})
		assert.DeepEqual(t, loginResponse.StatusCode, 403)
	})

	t.Run("Prevent double login", func(t *testing.T) {
		client := createTestClient(t, listeners)

		loginResponse := client.PostJSON(t, "/api/auth/session/login", map[string]interface{}{
			"Username": testUserName,
			"Password": testUserPassword,
		})
		assert.DeepEqual(t, loginResponse.StatusCode, 200)

		doubleLoginResponse := client.PostJSON(t, "/api/auth/session/login", map[string]interface{}{
			"Username": testUserName,
			"Password": testUserPassword,
		})
		assert.DeepEqual(t, doubleLoginResponse.StatusCode, 400)
	})
}

func createTestServer(t *testing.T) (*application.Server, application.ServerListeners, func()) {
	tmpDir := t.TempDir()

	config := application.Config{
		DataPath: tmpDir,

		HTTP: application.HTTPConfig{
			Enable: true,
		},

		GRPC: application.GRPCConfig{
			Enable: true,
		},
	}

	server, err := application.CreateServer(config)
	assert.NilError(t, err)

	listeners, err := config.Listen()
	assert.NilError(t, err)

	testContext, cancelTestContext := context.WithCancel(context.Background())
	go func() {
		err := server.Serve(testContext, listeners)
		assert.NilError(t, err)
	}()

	return server, listeners, cancelTestContext
}

func createTestClient(t *testing.T, listeners application.ServerListeners) customClient {
	jar, err := cookiejar.New(nil)
	assert.NilError(t, err)

	return customClient{
		client: &http.Client{
			Jar: jar,
		},
		listeners: listeners,
	}
}

type customClient struct {
	client    *http.Client
	listeners application.ServerListeners
}

func (c *customClient) PostJSON(t testing.TB, path string, body interface{}) *http.Response {
	bodyBytes, err := json.Marshal(body)
	assert.NilError(t, err)

	response, err := c.client.Post(c.httpURL(path), "application/json", bytes.NewReader(bodyBytes))
	assert.NilError(t, err)

	return response
}

func (c *customClient) httpURL(path string) string {
	return fmt.Sprintf("http://%s/%s", c.listeners.HTTP.Listeners[0].Addr().String(), strings.TrimPrefix(path, "/"))
}
