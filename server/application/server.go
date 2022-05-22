package application

func CreateServer(config Config) (*Server, error) {
	dbLocation := "./data/db/db.sqlite"
	repositoriesDir := "./data/repositories/files"

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
