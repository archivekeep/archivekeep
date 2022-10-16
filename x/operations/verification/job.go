package verification

import (
	"context"
	"fmt"
	"sort"
	"sync/atomic"
	"time"

	"github.com/kr/pretty"
	"golang.org/x/sync/errgroup"

	"github.com/archivekeep/archivekeep/internal/util"
)

type Job struct {
	archive VerifiableArchive

	previousState State
	options       JobOptions

	state   State
	started atomic.Bool
}

type JobOptions struct {
	Logger           pretty.Printfer
	LogVerifiedFiles bool

	ProgressSaver      func(currentState *State) error
	ProgressSavePeriod time.Duration
}

func NewJob(
	archiveReader VerifiableArchive,
	previousState *State,
	options JobOptions,
) *Job {
	return &Job{
		archive: archiveReader,

		previousState: State{
			correctFiles: previousState.correctFiles,
		},

		started: atomic.Bool{},
		state: State{
			correctFiles: map[string]successResult{},
			errorFiles:   map[string]errorResult{},
		},

		options: options,
	}
}

func (job *Job) Execute(
	ctx context.Context,
) (*State, error) {
	if !job.started.CompareAndSwap(false, true) {
		return nil, fmt.Errorf("job has been already started")
	}

	g, gCtx := errgroup.WithContext(ctx)
	monitoringContext, monitoringContextCancel := context.WithCancel(gCtx)

	g.Go(func() error {
		defer monitoringContextCancel()

		return job.execute(gCtx)
	})

	if job.options.ProgressSaver != nil {
		g.Go(func() error {
			return job.progressSaver(monitoringContext)
		})
	}

	return &job.state, g.Wait()
}

func (job *Job) progressSaver(ctx context.Context) error {
	progressSavePeriod := job.options.ProgressSavePeriod
	if progressSavePeriod == 0 {
		progressSavePeriod = 15 * time.Second
	}

	return util.RunPeriodicallyAndOnceAfterDone(
		ctx,
		progressSavePeriod,
		func() error {
			err := job.options.ProgressSaver(&job.state)
			if err != nil {
				return fmt.Errorf("progress save failed: %w", err)
			}
			return nil
		},
	)
}

func (job *Job) execute(
	ctx context.Context,
) error {
	filesToVerify, err := job.prepareListToVerify()
	if err != nil {
		return err
	}
	defer func() {
		job.options.Logger.Printf("OK files:        %d\n", len(job.state.correctFiles))
		job.options.Logger.Printf("Corrupted files: %d\n", len(job.state.errorFiles))
	}()

	job.options.Logger.Printf("Found %d files to verify\n", len(filesToVerify))

	for n, path := range filesToVerify {
		err := job.archive.VerifyFileIntegrity(path)

		if err != nil {
			job.onError(path, err)
		} else {
			job.onSuccess(path)
		}

		if (n+1)%10 == 0 || n+1 == len(filesToVerify) {
			job.options.Logger.Printf("Verified %d of %d files\n", n+1, len(filesToVerify))
		}

		select {
		case <-ctx.Done():
			job.options.Logger.Printf("Verification interrupted\n")
			return fmt.Errorf("interrupted")
		default:
		}
	}

	job.options.Logger.Printf("Verification completed\n")

	if corruptedFiles := job.state.numberOfCorruptedFiles(); corruptedFiles > 0 {
		return fmt.Errorf("found %d corrupted files", corruptedFiles)
	}

	return nil
}

func (job *Job) prepareListToVerify() ([]string, error) {
	var (
		threshold = time.Now().Add(-24 * time.Hour)
	)

	storedFiles, err := job.archive.StoredFiles()
	if err != nil {
		return nil, fmt.Errorf("retrieve current archive stored files: %w", err)
	}
	sort.Strings(storedFiles)

	filesToVerify := make([]string, 0, len(storedFiles))
	for _, filename := range storedFiles {
		correct, correctOK := job.previousState.correctFiles[filename]

		if correctOK && correct.VerifiedAt.After(threshold) {
			continue
		}

		filesToVerify = append(filesToVerify, filename)
	}

	sort.SliceStable(filesToVerify, func(i, j int) bool {
		iCorrect, iCorrectOK := job.previousState.correctFiles[filesToVerify[i]]
		jCorrect, jCorrectOK := job.previousState.correctFiles[filesToVerify[j]]

		if iCorrectOK && jCorrectOK {
			return iCorrect.VerifiedAt.Before(jCorrect.VerifiedAt)
		} else if !iCorrectOK && jCorrectOK {
			return true
		} else {
			return false
		}
	})

	return filesToVerify, nil
}

func (job *Job) onSuccess(path string) {
	if job.options.LogVerifiedFiles {
		job.options.Logger.Printf("INFO: valid file %s\n", path)
	}

	job.state.addSuccess(path)
}

func (job *Job) onError(path string, err error) {
	job.options.Logger.Printf("ERROR: corrupted %s: %v\n", path, err)

	job.state.addCorrupted(path, err)
}
