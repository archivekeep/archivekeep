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

type userContextDetails struct {
	userId          int64
	otherIdentities []string
}

func getLoggedInSubject(ctx context.Context) (int64, []string, bool) {
	userContextDetails, ok := ctx.Value(authenticatedUserIDContextKey).(userContextDetails)

	return userContextDetails.userId, userContextDetails.otherIdentities, ok
}

func NewContextWithAuthenticatedUser(ctx context.Context, user *User) context.Context {
	return context.WithValue(ctx, authenticatedUserIDContextKey, userContextDetails{
		userId:          user.ID,
		otherIdentities: []string{user.ByEmailResourceName()},
	})
}

type AuthenticationController struct {
	UserRepository *UserRepository
}

func (c *AuthenticationController) RegisterRoutes(r chi.Router) {
	r.Get("/auth/current-user", c.getCurrentUser)
}

func (c *AuthenticationController) getCurrentUser(w http.ResponseWriter, r *http.Request) {
	userID, _, loggedIn := getLoggedInSubject(r.Context())

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
