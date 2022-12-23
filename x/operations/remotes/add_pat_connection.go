package remotes

import (
	"github.com/archivekeep/archivekeep/archive/remote"
)

func AddPATConnection(
	remoteAddress string,
	personalAccessToken string,
	connectionsStore ConnectionsStore,
) error {
	return connectionsStore.Update(func(connections map[string]*remote.Connection) (map[string]*remote.Connection, error) {
		connections[remoteAddress] = &remote.Connection{
			Address: remoteAddress,

			BasicAuthCredentials: &remote.BasicAuthCredentials{
				Password: personalAccessToken,
			},
		}

		return connections, nil
	})
}
