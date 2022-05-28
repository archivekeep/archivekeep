package application

import (
	"errors"
	"log"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/archivekeep/archivekeep/server/api"
	"github.com/archivekeep/archivekeep/server/api/rest"
	"github.com/archivekeep/archivekeep/server/web/spa"
)

func createRouter(s *Server) *chi.Mux {
	restApi := rest.NewAPI(rest.Options{
		ArchiveService: s.ArchiveRestService,

		ErrorHandler: handleAppError,
	})

	authenticationController := AuthenticationController{
		UserRepository: s.UserRepository,
	}

	r := chi.NewRouter()
	r.Use(middleware.Logger)
	r.Use(middleware.Compress(5))
	r.Use(s.SessionBasedAuthentication.Middleware())

	r.Route("/api", func(r chi.Router) {
		r.Group(s.SessionBasedAuthentication.RegisterRoutes)
		r.Group(authenticationController.RegisterRoutes)

		r.Group(func(r chi.Router) {
			r.Use(requireLogin)

			restApi.Register(r)

			r.Post("/archives", func(w http.ResponseWriter, r *http.Request) {
				err := s.ArchiveRestService.CreateArchive(r.Context())
				if err != nil {
					handleAppError(w, err)
					return
				}

				w.WriteHeader(http.StatusOK)
			})
		})
	})

	if spa.HandlerFuncFactory != nil {
		r.Get("/*", spa.HandlerFuncFactory(spa.Config{
			PublicPath: s.Config.HTTP.PublicPath,
		}))
	}

	return r
}

func requireLogin(handler http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if _, loggedIN := getLoggedInUserID(r.Context()); !loggedIN {
			w.WriteHeader(http.StatusForbidden)
			return
		}

		handler.ServeHTTP(w, r)
	})
}

func handleAppError(w http.ResponseWriter, err error) {
	if errors.Is(err, api.ErrNotAuthorized) {
		log.Printf("forbidden access error: %v", err)
		w.WriteHeader(http.StatusNotFound)
	} else if errors.Is(err, api.ErrNotFound) {
		log.Printf("not found error: %v", err)
		w.WriteHeader(http.StatusNotFound)
	} else {
		log.Printf("internal error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
	}
}
