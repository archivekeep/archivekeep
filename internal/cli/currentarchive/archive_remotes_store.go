package currentarchive

import (
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

const remotesSecretKey = "remotes"

type keyringRemotesStore struct {
	Keyring *josesecrets.Keyring
}

func (j keyringRemotesStore) Get(name string) (*remote.Connection, error) {
	var connections map[string]*remote.Connection

	if !j.Keyring.Contains(remotesSecretKey) {
		return nil, nil
	}

	err := j.Keyring.GetTo(remotesSecretKey, &connections)
	if err != nil {
		return nil, fmt.Errorf("get connections from keyring: %w", err)
	}

	return connections[name], nil
}

func (j keyringRemotesStore) Update(f func(current map[string]*remote.Connection) (map[string]*remote.Connection, error)) error {
	return j.Keyring.Update(func(values map[string]json.RawMessage) error {
		var connections map[string]*remote.Connection

		if storedConnections, ok := values[remotesSecretKey]; ok {
			err := json.Unmarshal(storedConnections, &connections)
			if err != nil {
				return fmt.Errorf("unmarshal stored connections: %w", err)
			}
		} else {
			connections = map[string]*remote.Connection{}
		}

		connections, err := f(connections)
		if err != nil {
			return fmt.Errorf("preparing store update: %w", err)
		}

		newConnections, err := json.Marshal(connections)
		if err != nil {
			return fmt.Errorf("marshal new connections: %w", err)
		}

		values[remotesSecretKey] = newConnections

		return nil
	})
}
