package application

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"log"
	paths "path"
	"time"

	"github.com/allegro/bigcache/v3"
	"golang.org/x/image/draw"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/thumbnails"
	"github.com/archivekeep/archivekeep/server/api"
)

var (
	generator = thumbnails.NewGenerator(thumbnails.GeneratorOptions{
		Interpolator: draw.CatmullRom,
	})

	cache, _ = bigcache.NewBigCache(bigcache.Config{
		Shards:             512,
		LifeWindow:         5 * time.Minute,
		CleanWindow:        0,
		MaxEntriesInWindow: 1_000,
		MaxEntrySize:       40_000,
		HardMaxCacheSize:   1_000,
	})
)

type ownedArchiveAccessor struct {
	api.UnimplementedArchiveAccessor

	id  string
	arw archive.ReadWriter
}

func (a ownedArchiveAccessor) OpenReader() (archive.Reader, error) {
	return a.arw, nil
}

func (a ownedArchiveAccessor) OpenWriter() (archive.Writer, error) {
	return a.arw, nil
}

func (a ownedArchiveAccessor) OpenReadWriter() (archive.ReadWriter, error) {
	return a.arw, nil
}

func (a ownedArchiveAccessor) GetThumbnail(ctx context.Context, filePath string) (io.ReadCloser, error) {
	archiveReader, err := a.OpenReader()
	if err != nil {
		return nil, err
	}

	info, readCloser, err := archiveReader.OpenFile(filePath)
	if err != nil {
		return nil, err
	}
	defer readCloser.Close()

	cacheKey := info.Digest["SHA256"]

	thumbImgBytes, err := cache.Get(cacheKey)
	if err != nil {
		if !errors.Is(err, bigcache.ErrEntryNotFound) {
			log.Printf("WARN: can't get from cache: %v", err)
		}

		thumbImgBytes, err = generator.GenerateThumbnailJPEG(ctx, readCloser, paths.Ext(filePath))
		if err != nil {
			return nil, fmt.Errorf("generate thumbnail: %w", err)
		}

		err = cache.Set(cacheKey, thumbImgBytes)
		if err != nil {
			log.Printf("WARN: can't set cache item: %v", err)
		}
	}

	return io.NopCloser(bytes.NewReader(thumbImgBytes)), err
}
