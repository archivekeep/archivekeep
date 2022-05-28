package application

import (
	"log"
	"net"
)

// Server is an application server
type Server struct {
	UserRepository *UserRepository

	ArchiveService *ArchiveService
	UserService    *UserService

	ArchiveRestService         *archiveApplicationService
	SessionBasedAuthentication *SessionBasedAuthentication

	Config Config

	HTTP struct {
		Listeners []net.Listener
	}
}

func CreateServer(config Config) (*Server, error) {
	if config.DataPath == "" {
		config.DataPath = "./data"
		log.Printf("using default data path: " + config.DataPath)
	}

	dbLocation := config.DataPath + "/db/db.sqlite"
	repositoriesDir := config.DataPath + "/repositories/files"

	db, err := openOrCreateDB(dbLocation)
	if err != nil {
		return nil, err
	}

	var (
		contentStorage = &archiveContentStorage{
			rootDir: repositoriesDir,
		}

		archiveRepository = &sqlArchiveRepository{db: db}
		userRepository    = &UserRepository{db: db}
		sessionRepository = &SessionRepository{db: db}

		userService = &UserService{repository: userRepository}

		archiveService = &ArchiveService{
			archiveRepository: archiveRepository,
			contentStorage:    contentStorage,
		}

		sessionAuth = &SessionBasedAuthentication{
			UserService:       userService,
			SessionRepository: sessionRepository,
		}
	)

	service := &archiveApplicationService{
		archiveRepository: archiveRepository,
		contentStorage:    contentStorage,
	}

	return &Server{
		UserRepository: userRepository,

		ArchiveService: archiveService,
		UserService:    userService,

		ArchiveRestService:         service,
		SessionBasedAuthentication: sessionAuth,

		Config: config,
	}, nil
}
