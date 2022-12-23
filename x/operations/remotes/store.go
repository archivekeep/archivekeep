package remotes

import (
	"github.com/archivekeep/archivekeep/archive/remote"
)

type ConnectionsStore interface {
	Update(func(current map[string]*remote.Connection) (map[string]*remote.Connection, error)) error
}
