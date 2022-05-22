package application

import (
	"database/sql"
	_ "embed"
	"fmt"
	"os"
	paths "path"

	_ "github.com/mattn/go-sqlite3"
)

//go:embed schema.sql
var schemaSQL string

func openOrCreateDB(location string) (*sql.DB, error) {
	err := os.MkdirAll(paths.Dir(location), 0755)
	if err != nil {
		return nil, fmt.Errorf("create base dir for DB: %w", err)
	}

	db, err := sql.Open("sqlite3", location)
	if err != nil {
		return nil, fmt.Errorf("open DB")
	}

	_, err = db.Exec(schemaSQL)
	if err != nil {
		return nil, fmt.Errorf("init DB: %v", err)
	}
	return db, nil
}
