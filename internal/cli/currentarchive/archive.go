package currentarchive

import (
	"fmt"
	"os"
	paths "path"
	"path/filepath"

	"github.com/spf13/afero"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

type CurrentArchive struct {
	*filesarchive.Archive

	WorkingSubdirectory string
	RelativePathToRoot  string

	location string

	keyring *josesecrets.Keyring
}

func Open() (CurrentArchive, error) {
	wd, err := os.Getwd()
	if err != nil {
		return CurrentArchive{}, fmt.Errorf("get working directory: %w", err)
	}

	return OpenLocation(wd)
}

func OpenLocation(wd string) (CurrentArchive, error) {
	archivePath := wd

	currentArchive, err := filesarchive.Open(afero.NewOsFs(), archivePath)
	for err != nil && err.Error() == "archive does not exist" && archivePath != "/" {
		archivePath = paths.Dir(archivePath)
		currentArchive, err = filesarchive.Open(afero.NewOsFs(), archivePath)
	}
	if err != nil {
		return CurrentArchive{}, err
	}

	workingSubdirectory, err := filepath.Rel(archivePath, wd)
	if err != nil {
		return CurrentArchive{}, fmt.Errorf("compute relative path from archive root to working directory: %w", err)
	}

	pathToRoot, err := filepath.Rel(wd, archivePath)
	if err != nil {
		return CurrentArchive{}, fmt.Errorf("compute relative path from archive root to working directory: %w", err)
	}

	return CurrentArchive{
		Archive: currentArchive,

		WorkingSubdirectory: workingSubdirectory,
		RelativePathToRoot:  pathToRoot,

		location: archivePath,
	}, err
}

func OpenAndUnlockKeyring(
	passwordProvider func() (string, error),
) (CurrentArchive, error) {
	currentArchive, err := Open()
	if err != nil {
		return currentArchive, err
	}

	err = currentArchive.OpenKeyring(passwordProvider)
	if err != nil {
		return currentArchive, err
	}

	return currentArchive, nil
}

func (a *CurrentArchive) OpenKeyring(
	passwordProvider func() (string, error),
) error {
	password, err := passwordProvider()
	if err != nil {
		return fmt.Errorf("get password: %w", err)
	}

	keyring, err := josesecrets.OpenKeyring(paths.Join(a.location, ".archive", "secrets.json.jwe"), password)
	if err != nil {
		return err
	}
	a.keyring = keyring

	return nil
}

func (a *CurrentArchive) RemotesStore() *keyringRemotesStore {
	return &keyringRemotesStore{Keyring: a.keyring}
}

func (a *CurrentArchive) Keyring() *josesecrets.Keyring {
	return a.keyring
}

func (a *CurrentArchive) Location() string {
	return a.location
}

func (a *CurrentArchive) PrependWorkingDirectoryToPaths(relativePaths []string) []string {
	var inArchivePaths []string

	for _, relativePath := range relativePaths {
		inArchivePaths = append(inArchivePaths, filepath.Join(a.WorkingSubdirectory, relativePath))
	}

	return inArchivePaths
}
