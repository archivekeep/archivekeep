package datasizes

import (
	"testing"
)

func Run(b *testing.B, fn func(b *testing.B, sizeInBytes int64)) {
	benchmarks := []struct {
		name     string
		dataSize int64
	}{
		{"PDF_Small_30kB", 30_000},
		{"PDF_Medium_800kB", 800_000},
		{"PDF_Large_12MB", 12_000_000},
		{"Photo_JPG_5MB", 5_000_000},
		{"Photo_RAW_20MB", 20_000_000},
		{"CD_700MB", 700_000_000},
		{"DVD_4700MB", 4_700_000_000},
	}

	for _, bm := range benchmarks {
		b.Run(bm.name, func(b *testing.B) {
			// TODO: optionally enable memory expensive benchmarks
			if bm.dataSize > 1_000_000_000 {
				b.SkipNow()
			}

			fn(b, bm.dataSize)
		})
	}
}
