package sync

import (
	"context"
	"fmt"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/x/operations/comparison"
)

type Options struct {
	ResolveMoves             bool
	EnableDuplicateIncrease  bool
	EnableDuplicateReduction bool

	AdditiveDuplicating bool
}

func PerformSync(
	ctx context.Context,
	options Options,
	compareResult comparison.Result,
	fromArchive archive.Reader,
	toArchive archive.ReadWriter,
) error {
	compareResult, err := comparison.Execute(fromArchive, toArchive)
	if err != nil {
		return fmt.Errorf("prepare sync: %w", err)
	}

	if len(compareResult.Relocations) > 0 {
		if options.ResolveMoves && options.AdditiveDuplicating {
			return fmt.Errorf("use only one --resolve-moves or --additive-duplicating")
		}

		if options.AdditiveDuplicating {
			err := performAdditiveDuplicating(
				ctx,
				compareResult,
				fromArchive,
				toArchive,
			)

			if err != nil {
				return fmt.Errorf("copy extra files: %w", err)
			}
		} else if options.ResolveMoves {
			err := performRelocationsSync(
				ctx,
				compareResult,
				options,
				fromArchive,
				toArchive,
			)

			if err != nil {
				return fmt.Errorf("relocate moved files: %w", err)
			}
		} else {
			return fmt.Errorf("relocations detected, execute with --resolve-moves or --additive-duplicating")
		}
	}

	err = performNewFilesSync(
		ctx,
		compareResult,
		fromArchive,
		toArchive,
	)
	if err != nil {
		return fmt.Errorf("copy new files: %w", err)
	}

	return nil
}

func performAdditiveDuplicating(
	ctx context.Context,
	compareResult comparison.Result,
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
		}
	}

	return nil
}

func performRelocationsSync(
	ctx context.Context,
	compareResult comparison.Result,
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
		}
	}

	return nil
}

func performNewFilesSync(
	ctx context.Context,
	compareResult comparison.Result,
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
		}
	}

	return nil
}

func copyFile(target archive.Writer, source archive.Reader, filename string) error {
	fileInfo, fileReader, err := source.OpenFile(filename)
	if err != nil {
		return fmt.Errorf("open file: %w", err)

	}
	defer fileReader.Close()

	return target.SaveFile(fileReader, &fileInfo)
}
