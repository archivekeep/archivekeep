package remotes

import (
	"encoding/json"
	"fmt"

	"github.com/archivekeep/archivekeep/archive/remote"
)

func AddConnection(
	name string,
	tokenBytes []byte,
	connectionsStore ConnectionsStore,
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

	return connectionsStore.Update(func(connections map[string]*remote.Connection) (map[string]*remote.Connection, error) {
		connections[name] = connection

		return connections, nil
	})
}
