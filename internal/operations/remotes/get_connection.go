package remotes

import (
	"fmt"

	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
)

func GetConnection(name string, keyring *josesecrets.Keyring) (*remote.Connection, error) {
	var connections map[string]*remote.Connection

	if !keyring.Contains(remotesSecretKey) {
		return nil, nil
	}

	err := keyring.GetTo(remotesSecretKey, &connections)
	if err != nil {
		return nil, fmt.Errorf("get connections from keyring: %w", err)
	}

	return connections[name], nil
}
