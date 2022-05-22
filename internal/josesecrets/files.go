package josesecrets

import (
	"encoding/json"
	"fmt"
	"os"
	paths "path"
	"strconv"
	"time"

	"github.com/archivekeep/archivekeep/internal/util"
)

const (
	directoryCreationPerm = 0755
	fileCreationPerm      = 0644
)

func SaveToFile(fileName string, value interface{}, password string) error {
	// TODO: cover possible error scenarios with tests

	backupKeyringPath := fileName + "~" + strconv.FormatInt(time.Now().Unix(), 10)
	if util.FileExists(fileName) {
		err := os.Rename(
			fileName,
			backupKeyringPath,
		)
		if err != nil {
			return fmt.Errorf("move old keyring: %w", err)
		}
	}

	err := os.MkdirAll(paths.Dir(fileName), directoryCreationPerm)
	if err != nil {
		return fmt.Errorf("mkdir for keyring: %w", err)
	}

	f, err := os.OpenFile(fileName, os.O_WRONLY|os.O_CREATE|os.O_EXCL, fileCreationPerm)
	if err != nil {
		return fmt.Errorf("open keyring for save: %w", err)
	}
	defer f.Close()

	err = MarshalWrite(f, value, password)
	if err != nil {
		return err
	}

	if util.FileExists(backupKeyringPath) {
		return os.Remove(backupKeyringPath)
	}

	return nil
}

func UpdateFile(
	fileName string,
	updateFn func(values map[string]json.RawMessage) error,
	password string,
) (map[string]json.RawMessage, error) {
	// TODO: cover possible error scenarios with tests

	value := map[string]json.RawMessage{}
	backupKeyringPath := fileName + "~" + strconv.FormatInt(time.Now().Unix(), 10)
	if util.FileExists(fileName) {
		err := os.Rename(
			fileName,
			backupKeyringPath,
		)
		if err != nil {
			return nil, fmt.Errorf("move old keyring: %w", err)
		}

		err = ReadUnmarshalFile(&value, backupKeyringPath, password)
		if err != nil {
			return nil, fmt.Errorf("read current: %w", err)
		}
	}

	err := os.MkdirAll(paths.Dir(fileName), directoryCreationPerm)
	if err != nil {
		return nil, fmt.Errorf("mkdir for keyring: %w", err)
	}

	f, err := os.OpenFile(fileName, os.O_WRONLY|os.O_CREATE|os.O_EXCL, fileCreationPerm)
	if err != nil {
		return nil, fmt.Errorf("open keyring for save: %w", err)
	}
	defer f.Close()

	err = updateFn(value)
	if err != nil {
		return nil, err
	}

	err = MarshalWrite(f, value, password)
	if err != nil {
		return nil, err
	}

	if util.FileExists(backupKeyringPath) {
		return value, os.Remove(backupKeyringPath)
	}

	return value, nil
}
