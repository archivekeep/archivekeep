package application

import (
	"log"
	"net"
)

// Server is an application server
type Server struct {
	UserRepository *UserRepository

	ArchiveService                *ArchiveService
	PersonalAccessTokenRepository *PersonalAccessTokenRepository
	UserService                   *UserService

	ArchiveApplicationService             *archiveApplicationService
	ArchivePermissionApplicationService   *archivePermissionApplicationService
	PersonalAccessTokenApplicationService *personalAccessTokenApplicationService

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

		archiveRepository             = &sqlArchiveRepository{db: db}
		archivePermissionRepository   = &sqlArchivePermissionRepository{db: db}
		personalAccessTokenRepository = &PersonalAccessTokenRepository{db: db}
		sessionRepository             = &SessionRepository{db: db}
		userRepository                = &UserRepository{db: db}

		userService = &UserService{repository: userRepository}

		archiveService = &ArchiveService{
			archiveRepository: archiveRepository,
			contentStorage:    contentStorage,
		}

		personalAccessTokenApplicationService = &personalAccessTokenApplicationService{
			personalAccessTokenRepository: personalAccessTokenRepository,
			userRepository:                userRepository,
		}

		sessionAuth = &SessionBasedAuthentication{
			UserService:       userService,
			UserRepository:    userRepository,
			SessionRepository: sessionRepository,
		}
	)

	service := &archiveApplicationService{
		archiveRepository:           archiveRepository,
		archivePermissionRepository: archivePermissionRepository,
		contentStorage:              contentStorage,
	}

	archivePermissionService := &archivePermissionApplicationService{
		archiveRepository:           archiveRepository,
		archivePermissionRepository: archivePermissionRepository,
	}

	return &Server{
		UserRepository: userRepository,

		ArchiveService:                archiveService,
		PersonalAccessTokenRepository: personalAccessTokenRepository,
		UserService:                   userService,

		ArchiveApplicationService:             service,
		ArchivePermissionApplicationService:   archivePermissionService,
		PersonalAccessTokenApplicationService: personalAccessTokenApplicationService,

		SessionBasedAuthentication: sessionAuth,

		Config: config,
	}, nil
}
