package rest

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	paths "path"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/archivekeep/archivekeep/internal/thumbnails"
	"github.com/archivekeep/archivekeep/server/api"
)

const ArchiveIdResourceParam = "archiveID"

type Options struct {
	ArchiveService api.ArchiveService

	ErrorHandler func(w http.ResponseWriter, err error)
}

type API struct {
	options Options
}

func NewAPI(options Options) *API {
	return &API{
		options: options,
	}
}

func (s *API) Handler() http.Handler {
	r := chi.NewRouter()
	r.Use(middleware.Logger)

	r.Group(s.Register)

	return r
}

func (s *API) Register(r chi.Router) {
	r.Route("/archives", func(r chi.Router) {
		r.Get("/", s.listArchives)

		r.Route(
			fmt.Sprintf("/{%s}", ArchiveIdResourceParam),
			func(r chi.Router) {
				r.Get("/", s.getArchive)

				r.Get("/index", s.listArchiveFiles)

				r.Get("/files/*", s.getFile)
				r.Get("/thumbnails/*", s.getThumbnail)
			},
		)
	})
}

func (s *API) listArchives(w http.ResponseWriter, r *http.Request) {
	aa, err := s.options.ArchiveService.ListArchives(r.Context(), api.ListArchivesRequest{})
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("list archives: %w", err))
		return
	}

	var dto []ArchiveDTO
	for _, a := range aa.Archives {
		dto = append(dto, archiveToDTO(a))
	}

	s.writeJSONResponse(w, dto)
}

func (s *API) getArchive(w http.ResponseWriter, r *http.Request) {
	archiveID := chi.URLParam(r, ArchiveIdResourceParam)

	a, err := s.options.ArchiveService.GetArchive(r.Context(), archiveID)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get archive with id %s: %w", archiveID, err))
		return
	}

	s.writeJSONResponse(w, archiveToDTO(a))
}

func (s *API) listArchiveFiles(w http.ResponseWriter, r *http.Request) {
	archiveID := chi.URLParam(r, ArchiveIdResourceParam)

	a, err := s.options.ArchiveService.GetArchiveAccessor(r.Context(), archiveID)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get archive with id %s: %w", archiveID, err))
		return
	}

	archiveReader, err := a.OpenReader()
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("open archive for reading: %w", err))
		return
	}

	files, err := archiveReader.ListFiles()
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("list archive contents id %s: %w", archiveID, err))
		return
	}

	s.writeJSONResponse(w, files)
}

func (s *API) getFile(w http.ResponseWriter, r *http.Request) {
	archiveID := chi.URLParam(r, ArchiveIdResourceParam)
	filePath := chi.URLParam(r, "*")

	a, err := s.options.ArchiveService.GetArchiveAccessor(r.Context(), archiveID)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get archive with id %s: %w ", archiveID, err))
		return
	}

	archiveReader, err := a.OpenReader()
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("open archive for reading: %w", err))
		return
	}

	_, reader, err := archiveReader.OpenFile(filePath)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get archive file %s: %w", filePath, err))
		return
	}
	if reader == nil {
		s.options.ErrorHandler(w, fmt.Errorf("read %s: %w", filePath, api.ErrNotFound))
		return
	}
	defer reader.Close()

	w.WriteHeader(http.StatusOK)
	io.Copy(w, reader)
}

var (
	fallbackThumbnailGenerator = thumbnails.NewGenerator(thumbnails.GeneratorOptions{})
)

func (s *API) getThumbnail(w http.ResponseWriter, r *http.Request) {
	archiveID := chi.URLParam(r, ArchiveIdResourceParam)
	filePath := chi.URLParam(r, "*")

	a, err := s.options.ArchiveService.GetArchiveAccessor(r.Context(), archiveID)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get archive with id %s: %w ", archiveID, err))
		return
	}

	thumbImgReader, err := a.GetThumbnail(r.Context(), filePath)
	if errors.Is(err, api.ErrNotImplemented) {
		log.Printf("WARN: application service doesn't implement thumbnail generation, using fallback generator")

		thumbImgReader, err = s.generateFallbackThumbnail(r.Context(), a, filePath)
		if err != nil {
			s.options.ErrorHandler(w, fmt.Errorf("generate fallback thumbnail: %w", err))
			return
		}
	} else if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("get thumbnail: %w", err))
		return
	}

	defer thumbImgReader.Close()

	w.WriteHeader(http.StatusOK)
	io.Copy(w, thumbImgReader)
}

func (s *API) generateFallbackThumbnail(ctx context.Context, a api.ArchiveAccessor, filePath string) (io.ReadCloser, error) {
	archiveReader, err := a.OpenReader()
	if err != nil {
		return nil, fmt.Errorf("open archive for reading: %w", err)
	}

	_, readCloser, err := archiveReader.OpenFile(filePath)
	if err != nil {
		return nil, fmt.Errorf("open file from archive: %w", err)
	}
	defer readCloser.Close()

	thumbImgBytes, err := fallbackThumbnailGenerator.GenerateThumbnailJPEG(ctx, readCloser, paths.Ext(filePath))
	if err != nil {
		return nil, fmt.Errorf("generate thumbnail: %w", err)
	}

	return io.NopCloser(bytes.NewReader(thumbImgBytes)), nil
}

func archiveToDTO(a api.ArchiveDetails) ArchiveDTO {
	return ArchiveDTO{
		ID:   a.ID,
		Name: a.ArchiveName,
	}
}

func (s *API) writeJSONResponse(w http.ResponseWriter, dto interface{}) {
	result, err := json.Marshal(dto)
	if err != nil {
		s.options.ErrorHandler(w, fmt.Errorf("marshal json: %w", err))
		return
	}

	w.WriteHeader(http.StatusOK)
	_, err = w.Write(result)
	if err != nil {
		log.Printf("ERROR - writer result: %v", err)
	}
}
