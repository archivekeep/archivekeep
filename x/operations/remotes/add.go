package remotes

import (
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

const remotesSecretKey = "remotes"

func AddConnection(
	name string,
	tokenBytes []byte,
	secretsStore *josesecrets.Keyring,
) error {
	var token remote.ConnectTokenJSON

	err := json.Unmarshal(tokenBytes, &token)
	if err != nil {
		return fmt.Errorf("unmarshal token: %w", err)
	}

	connection, err := token.ToConnection()
	if err != nil {
		return fmt.Errorf("convert to connection: %w", err)
	}

	return secretsStore.Update(func(values map[string]json.RawMessage) error {
		var connections map[string]*remote.Connection

		if storedConnections, ok := values[remotesSecretKey]; ok {
			err := json.Unmarshal(storedConnections, &connections)
			if err != nil {
				return fmt.Errorf("unmarshal stored connections: %w", err)
			}
		} else {
			connections = map[string]*remote.Connection{}
		}

		connections[name] = connection

		newConnections, err := json.Marshal(connections)
		if err != nil {
			return fmt.Errorf("marshal new connections: %w", err)
		}

		values[remotesSecretKey] = newConnections

		return nil
	})
}
