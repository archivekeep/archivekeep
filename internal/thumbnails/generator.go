package thumbnails

import (
	"context"
	"fmt"
	"image"
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
}

func NewGenerator(options GeneratorOptions) *Generator {
	if options.MaxParallel == 0 {
		options.MaxParallel = int64(runtime.NumCPU())
	}
	if options.Interpolator == nil {
		options.Interpolator = draw.ApproxBiLinear
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
