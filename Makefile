FLAGS := -tags embed_version

build:
	./bin/generate-version.sh
	go build $(FLAGS)

install:
	./bin/generate-version.sh
	go install $(FLAGS)
