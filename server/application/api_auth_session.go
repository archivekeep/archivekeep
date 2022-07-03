package application

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/gorilla/securecookie"
	"golang.org/x/crypto/bcrypt"
)

const sessionCookieName = "session"
const sessionTokenName = "sessionToken"

var (
	sessionContextKey = contextKey("session")
)

// TODO: add some cookie encrypter
var sessionSecureCookie = securecookie.New(
	securecookie.GenerateRandomKey(64),
	securecookie.GenerateRandomKey(32),
)

type SessionBasedAuthentication struct {
	UserService       *UserService
	SessionRepository *SessionRepository
}

func (a *SessionBasedAuthentication) Middleware() func(http.Handler) http.Handler {
	return func(handler http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			done := a.loadSession(w, r)
			if done {
				return
			}

			handler.ServeHTTP(w, r)
		})
	}
}

func (a *SessionBasedAuthentication) RegisterRoutes(r chi.Router) {
	r.Post("/auth/session/login", a.handleLogin)
	r.Post("/auth/session/logout", a.handleLogout)
}

func (a *SessionBasedAuthentication) loadSession(w http.ResponseWriter, r *http.Request) bool {
	sessionToken, sessionIDPresent := "", false
	var session map[string]string

	cookie, err := r.Cookie(sessionCookieName)
	if err != nil {
		if err == http.ErrNoCookie {
			err := setSessionCookie(w, map[string]string{})
			if err != nil {
				log.Printf("error setting session cookie: %v", err)
				w.WriteHeader(http.StatusInternalServerError)
				return true
			}
		} else {
			log.Printf("error reading session cookie: %v", err)
			w.WriteHeader(http.StatusBadRequest)
			return true
		}
	} else {
		value := map[string]string{}

		err = sessionSecureCookie.Decode(sessionCookieName, cookie.Value, &value)
		if err != nil {
			log.Printf("error decoding session cookie: %v", err)

			err := setSessionCookie(w, map[string]string{})
			if err != nil {
				log.Printf("error setting session cookie: %v", err)
				w.WriteHeader(http.StatusInternalServerError)
				return true
			}
		}

		session = value
	}

	sessionToken, sessionIDPresent = session[sessionTokenName]

	if sessionIDPresent {
		session, err := a.SessionRepository.GetSessionByToken(sessionToken)
		if err != nil {
			log.Printf("error getting session: %v", err)
			w.WriteHeader(http.StatusInternalServerError)
			return true
		}

		if session != nil {
			ctx := r.Context()
			ctx = UserIDContext(ctx, session.UserID)
			ctx = context.WithValue(ctx, sessionContextKey, session)

			req := r.WithContext(ctx)
			*r = *req
		}
	}

	return false
}

func (a *SessionBasedAuthentication) handleLogin(w http.ResponseWriter, r *http.Request) {
	if _, currentlyLoggedIN := getLoggedInUserID(r.Context()); currentlyLoggedIN {
		log.Printf("already logged in")
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	var input struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}

	err := json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	user, err := a.UserService.VerifyLogin(r.Context(), input.Username, input.Password)
	if err != nil {
		log.Printf("error login: %v", err)
		if errors.Is(err, bcrypt.ErrMismatchedHashAndPassword) {
			w.WriteHeader(http.StatusForbidden)
		} else if errors.Is(err, errDbNotExist) {
			w.WriteHeader(http.StatusForbidden)
		} else {
			handleAppError(w, err)
		}
		return
	}

	err = a.startAuthSession(w, r, user)
	if err != nil {
		log.Printf("error starting session: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (a *SessionBasedAuthentication) handleLogout(w http.ResponseWriter, r *http.Request) {
	_, currentlyLoggedIN := getLoggedInUserID(r.Context())
	if !currentlyLoggedIN {
		log.Printf("not logged in")
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	session, ok := r.Context().Value(sessionContextKey).(*Session)
	if !ok {
		log.Printf("not logged in via session")
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	err := a.SessionRepository.Delete(session.ID)
	if err != nil {
		handleAppError(w, err)
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (a *SessionBasedAuthentication) startAuthSession(w http.ResponseWriter, r *http.Request, user *User) error {
	var session map[string]string

	cookie, err := r.Cookie(sessionCookieName)
	if err != nil {
		if err == http.ErrNoCookie {
			session = map[string]string{}
		} else {
			return fmt.Errorf("error reading session cookie: %v", err)
		}
	} else {
		value := map[string]string{}

		err = sessionSecureCookie.Decode(sessionCookieName, cookie.Value, &value)
		if err != nil {
			log.Printf("error decoding session cookie: %v", err)

			session = map[string]string{}
		} else {
			session = value
		}
	}

	token, err := a.SessionRepository.Create(user.ID)
	if err != nil {
		return fmt.Errorf("create session: %w", err)
	}

	session[sessionTokenName] = token

	err = setSessionCookie(w, session)
	if err != nil {
		return fmt.Errorf("set session cookie: %w", err)
	}

	return nil
}

func setSessionCookie(w http.ResponseWriter, value map[string]string) error {
	encoded, err := sessionSecureCookie.Encode(sessionCookieName, value)
	if err != nil {
		return err
	}

	cookie := &http.Cookie{
		Name:  sessionCookieName,
		Value: encoded,
		Path:  "/",
		// TODO: Secure:   true,
		HttpOnly: true,
	}
	http.SetCookie(w, cookie)

	return nil
}
