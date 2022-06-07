package thumbnails

import (
	"bytes"
	"context"
	"fmt"
	"image"
	"image/jpeg"
	"io"
	"runtime"
	"strings"

	"golang.org/x/image/draw"
	"golang.org/x/sync/semaphore"
)

type Generator struct {
	semaphore *semaphore.Weighted
	options   GeneratorOptions
}

type GeneratorOptions struct {
	MaxParallel  int64
	Interpolator draw.Interpolator

	JPEGQuality int
}

func NewGenerator(options GeneratorOptions) *Generator {
	if options.MaxParallel == 0 {
		options.MaxParallel = int64(runtime.NumCPU())
	}
	if options.Interpolator == nil {
		options.Interpolator = draw.ApproxBiLinear
	}
	if options.JPEGQuality == 0 {
		options.JPEGQuality = jpeg.DefaultQuality
	}

	return &Generator{
		semaphore: semaphore.NewWeighted(options.MaxParallel),
		options:   options,
	}
}

func (g *Generator) GenerateThumbnail(ctx context.Context, readCloser io.ReadCloser, extension string) (image.Image, error) {
	err := g.semaphore.Acquire(ctx, 1)
	if err != nil {
		return nil, fmt.Errorf("acquire semaphore: %w", err)
	}
	defer g.semaphore.Release(1)

	extension = strings.ToLower(extension)

	switch extension {
	case ".jpg", ".jpeg", ".png":
		break
	default:
		return nil, fmt.Errorf("extension %s is not supported", extension)
	}

	img, _, err := image.Decode(readCloser)
	if err != nil {
		return nil, fmt.Errorf("decode image: %w", err)
	}

	width, height := 480, 360
	origWidth, origHeight := img.Bounds().Size().X, img.Bounds().Size().Y
	baseWidth, baseHeight := origWidth, origHeight

	if width > baseWidth {
		width = baseWidth
		width = width - width%4
		height = (width * 3) / 4
	}
	if height > baseHeight {
		height = baseHeight
		height = height - height%3
		width = (height * 4) / 3
	}

	if baseWidth > baseHeight*4/3 {
		baseWidth = (baseHeight * 4) / 3
	}
	if baseHeight > baseWidth*3/4 {
		baseHeight = (baseWidth * 3) / 4
	}

	resultImg := image.NewRGBA(image.Rect(0, 0, width, height))

	offsetX := (origWidth - baseWidth) / 2
	offsetY := (origHeight - baseHeight) / 2

	g.options.Interpolator.Scale(
		resultImg, resultImg.Rect,
		img, image.Rect(offsetX, offsetY, offsetX+baseWidth, offsetY+baseHeight),
		draw.Src, nil,
	)

	return resultImg, nil
}

func (g *Generator) GenerateThumbnailJPEG(ctx context.Context, readCloser io.ReadCloser, extension string) ([]byte, error) {
	thumbImg, err := g.GenerateThumbnail(ctx, readCloser, extension)
	if err != nil {
		return nil, fmt.Errorf("generate image: %w", err)
	}

	var buf bytes.Buffer
	err = jpeg.Encode(&buf, thumbImg, &jpeg.Options{
		Quality: g.options.JPEGQuality,
	})
	if err != nil {
		return nil, fmt.Errorf("encode to JPEG: %w", err)
	}

	return buf.Bytes(), nil
}
