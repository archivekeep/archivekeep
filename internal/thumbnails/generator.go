package thumbnails

import (
	"fmt"
	"image"
	paths "path"
	"strings"

	"golang.org/x/image/draw"

	"github.com/archivekeep/archivekeep/archive"
)

type Generator struct {
	ArchiveReader archive.Reader
}

func (g *Generator) GenerateThumbnail(filePath string) (image.Image, error) {
	extension := strings.ToLower(paths.Ext(filePath))
	switch extension {
	case ".jpg", ".jpeg", ".png":
		break
	default:
		return nil, fmt.Errorf("extension %s is not supported", extension)
	}

	_, readCloser, err := g.ArchiveReader.OpenFile(filePath)
	if err != nil {
		return nil, fmt.Errorf("open file from archive: %w", err)
	}
	defer readCloser.Close()

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
	draw.CatmullRom.Scale(
		resultImg, resultImg.Rect,
		img, image.Rect(offsetX, offsetY, offsetX+baseWidth, offsetY+baseHeight),
		draw.Src, nil,
	)

	return resultImg, nil
}
