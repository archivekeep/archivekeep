package verification

import (
	"context"
	"fmt"

	"github.com/kr/pretty"

	"github.com/archivekeep/archivekeep/archive"
)

type ActionOptions struct {
	Logger           pretty.Printfer
	LogVerifiedFiles bool

	StartFromScratch bool
}

type VerifiableArchive interface {
	StoredFiles() ([]string, error)
	VerifyFileIntegrity(path string) error

	LoadVerificationState() (*State, error)
	SaveVerificationState(state *State) error
}

func Execute(
	ctx context.Context,
	reader archive.Reader,
	options ActionOptions,
) (*State, error) {
	verifiableArchive, ok := reader.(VerifiableArchive)
	if !ok {
		return nil, fmt.Errorf("archive is not verifiable: got %T", reader)
	}

	previousState, err := verifiableArchive.LoadVerificationState()
	if err != nil {
		return nil, fmt.Errorf("cant't load verification state: %w", err)
	}
	if options.StartFromScratch {
		previousState = &State{}
	}

	jobOptions := JobOptions{
		Logger:           options.Logger,
		LogVerifiedFiles: options.LogVerifiedFiles,

		ProgressSaver: verifiableArchive.SaveVerificationState,
	}

	job := NewJob(
		verifiableArchive,
		previousState,
		jobOptions,
	)

	return job.Execute(ctx)
}
