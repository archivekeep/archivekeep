package sync

import (
	"context"
	"fmt"

	"github.com/kr/pretty"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/operations/compare"
)

type Options struct {
	ResolveMoves             bool
	EnableDuplicateIncrease  bool
	EnableDuplicateReduction bool

	AdditiveDuplicating bool
}

func PerformAdditiveDuplicating(
	ctx context.Context,
	logger pretty.Printfer,
	compareResult compare.Result,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	for _, move := range compareResult.Relocations {
		for _, fileToCopy := range move.MissingNewFileNames {
			if ctx.Err() != nil {
				return fmt.Errorf("context error: %w", ctx.Err())
			}

			err := copyFile(toArchive, fromArchive, fileToCopy)
			if err != nil {
				return fmt.Errorf("transfer file %s: %w", fileToCopy, err)
			}

			logger.Printf("file copied: %s\n", fileToCopy)
		}
	}

	return nil
}

func PerformRelocationsSync(
	ctx context.Context,
	logger pretty.Printfer,
	compareResult compare.Result,
	options Options,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	for _, move := range compareResult.Relocations {
		if move.IsIncreasingDuplicates() {
			if !options.EnableDuplicateIncrease {
				return fmt.Errorf("use --allow-duplicate-increase")
			}

			for _, extraDuplication := range move.MissingNewFileNames[len(move.ExtraOriginalFileNames):] {
				if ctx.Err() != nil {
					return fmt.Errorf("context error: %w", ctx.Err())
				}

				// TODO: doesn't need copy across archives

				err := copyFile(toArchive, fromArchive, extraDuplication)
				if err != nil {
					return fmt.Errorf("create duplication %s: %w", extraDuplication, err)
				}

				logger.Printf("copied: %s\n", extraDuplication)
			}
		}

		if move.IsDecreasingDuplicates() {
			if !options.EnableDuplicateReduction {
				return fmt.Errorf("use --allow-duplicate-reduction")
			}

			for _, extraDuplication := range move.ExtraOriginalFileNames[len(move.MissingNewFileNames):] {
				if ctx.Err() != nil {
					return fmt.Errorf("context error: %w", ctx.Err())
				}

				err := toArchive.DeleteFile(extraDuplication)
				if err != nil {
					return fmt.Errorf("failed to remove extra duplication %s: %w", extraDuplication, err)
				}

				logger.Printf("deleted: %s\n", extraDuplication)
			}
		}

		for i := 0; i < len(move.ExtraOriginalFileNames) && i < len(move.MissingNewFileNames); i++ {
			if ctx.Err() != nil {
				return fmt.Errorf("context error: %w", ctx.Err())
			}

			from, to := move.ExtraOriginalFileNames[i], move.MissingNewFileNames[i]

			err := toArchive.MoveFile(from, to)
			if err != nil {
				return fmt.Errorf("failed to move %s -> %s: %w", from, to, err)
			}

			logger.Printf("moved: %s -> %s\n", from, to)
		}
	}

	return nil
}

func PerformNewFilesSync(
	ctx context.Context,
	cmd pretty.Printfer,
	compareResult compare.Result,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	for _, sourceExtraGroup := range compareResult.UnmatchedBaseExtras {
		for _, fileToPush := range sourceExtraGroup.Filenames {
			if ctx.Err() != nil {
				return fmt.Errorf("context error: %w", ctx.Err())
			}

			err := copyFile(toArchive, fromArchive, fileToPush)
			if err != nil {
				return fmt.Errorf("transfer file %s: %w", fileToPush, err)
			}

			cmd.Printf("file copied: %s\n", fileToPush)
		}
	}

	return nil
}

func copyFile(target archive.Writer, source archive.Reader, filename string) error {
	fileInfo, fileReader, err := source.OpenFile(filename)
	if err != nil {
		return fmt.Errorf("open file to push: %w", err)

	}
	defer fileReader.Close()

	return target.SaveFile(fileReader, &fileInfo)
}
