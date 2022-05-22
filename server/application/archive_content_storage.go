package application

import (
	"fmt"
	"path"

	"github.com/spf13/afero"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/archive/local/driver/plain"
)

var (
	afs = afero.NewOsFs()
)

type archiveContentStorage struct {
	rootDir string
}

func (storage *archiveContentStorage) get(reference string) (archive.ReadWriter, error) {
	arw, err := filesarchive.Open(afs, path.Join(storage.rootDir, reference))
	if err != nil {
		return nil, fmt.Errorf("open repository: %w", err)
	}

	return arw, nil
}

func (storage *archiveContentStorage) create(reference string) error {
	_, err := filesarchive.Init(afero.NewOsFs(), fmt.Sprintf("%s/%s", storage.rootDir, reference))
	if err != nil {
		return fmt.Errorf("init files archive: %w", err)
	}

	return nil
}
