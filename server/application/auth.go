package application

import (
	"context"
	"encoding/json"
	"log"
	"net/http"

	"github.com/go-chi/chi/v5"
)

type contextKey string

var (
	authenticatedUserIDContextKey = contextKey("authenticatedUserID")
)

func getLoggedInUserID(ctx context.Context) (int64, bool) {
	userId, ok := ctx.Value(authenticatedUserIDContextKey).(int64)

	return userId, ok
}

func UserIDContext(ctx context.Context, userID int64) context.Context {
	return context.WithValue(ctx, authenticatedUserIDContextKey, userID)
}

type AuthenticationController struct {
	UserRepository *UserRepository
}

func (c *AuthenticationController) RegisterRoutes(r chi.Router) {
	r.Get("/auth/current-user", c.getCurrentUser)
}

func (c *AuthenticationController) getCurrentUser(w http.ResponseWriter, r *http.Request) {
	userID, loggedIn := getLoggedInUserID(r.Context())

	if !loggedIn {
		w.WriteHeader(http.StatusNotFound)

		err := json.NewEncoder(w).Encode(struct {
			ErrorMessage string `json:"errorMessage"`
		}{
			ErrorMessage: "User not logged in",
		})
		handleResponseWriteErr(err)
		return
	}

	user, err := c.UserRepository.GetUserByUserID(userID)
	if err != nil {
		log.Printf("error getting user: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)

	err = json.NewEncoder(w).Encode(struct {
		ID    int64  `json:"id"`
		Email string `json:"email"`
	}{
		ID:    user.ID,
		Email: user.Email,
	})
	handleResponseWriteErr(err)
}

func handleResponseWriteErr(err error) {
	if err != nil {
		log.Printf("error writting response: %v", err)
	}
}
