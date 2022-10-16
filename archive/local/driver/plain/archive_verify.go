package filesarchive

import (
	"encoding/json"
	"fmt"
	"os"
	"path"

	"github.com/spf13/afero"

	"github.com/archivekeep/archivekeep/x/operations/verification"
)

var (
	verificationTmpFileName    = path.Join(".archive", "verification.json.tmp")
	verificationResultFileName = path.Join(".archive", "verification.json")
)

func (archive *Archive) LoadVerificationState() (*verification.State, error) {
	exists, err := archive.rootFs.Exists(verificationResultFileName)
	if err != nil {
		return nil, fmt.Errorf("check verification state existence: %w", err)
	}
	if !exists {
		return &verification.State{}, nil
	}

	data, err := archive.rootFs.ReadFile(verificationResultFileName)
	if err != nil {
		return nil, fmt.Errorf("read verification state: %w", err)
	}

	var state verification.State

	err = json.Unmarshal(data, &state)
	if err != nil {
		return &verification.State{}, fmt.Errorf("unmarshal state JSON: %w", err)
	}

	return &state, nil
}

func (archive *Archive) SaveVerificationState(state *verification.State) error {
	var (
		success = false
	)

	defer func() {
		if !success {
			if exists, _ := archive.rootFs.Exists(verificationTmpFileName); exists {
				archive.rootFs.Remove(verificationTmpFileName)
			}
		}
	}()

	stateJson, err := json.Marshal(state)
	if err != nil {
		return fmt.Errorf("marshal verification state: %w", err)
	}

	err = safeWriteFile(archive.rootFs, verificationTmpFileName, stateJson)
	if err != nil {
		return fmt.Errorf("save verification state into temp file: %w", err)
	}

	err = archive.rootFs.Rename(verificationTmpFileName, verificationResultFileName)
	if err != nil {
		return fmt.Errorf("replace old state with new temp file: %w", err)
	}

	return nil
}

func safeWriteFile(fs afero.Afero, name string, contents []byte) error {
	f, err := fs.OpenFile(name, os.O_RDWR|os.O_CREATE|os.O_EXCL, 0600)
	defer f.Close()

	_, err = f.Write(contents)
	if err != nil {
		return fmt.Errorf("write contents: %w", err)
	}

	err = f.Close()
	if err != nil {
		return fmt.Errorf("close: %w", err)
	}

	return nil
}
