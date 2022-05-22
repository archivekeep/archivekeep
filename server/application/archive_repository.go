package application

import (
	"database/sql"
	"fmt"
	"strconv"

	filesarchive "github.com/archivekeep/archivekeep/archive/local/driver/plain"
)

type dbArchive struct {
	ID    int64
	Owner string
	Name  string
}

func (a dbArchive) StringID() string {
	return strconv.FormatInt(a.ID, 10)
}

type sqlArchiveRepository struct {
	db *sql.DB
}

func (repository *sqlArchiveRepository) get(id int64) (dbArchive, error) {
	rows, err := repository.db.Query("SELECT id, owner, name FROM archive WHERE id = ?", id)
	if err != nil {
		return dbArchive{}, fmt.Errorf("query archives: %w", err)
	}
	defer rows.Close()

	for rows.Next() {
		archive := dbArchive{}

		err = rows.Scan(
			&archive.ID,
			&archive.Owner,
			&archive.Name,
		)
		if err != nil {
			return dbArchive{}, fmt.Errorf("scan archive row: %w", err)
		}

		return archive, nil
	}

	return dbArchive{}, errDbNotExist
}

func (repository *sqlArchiveRepository) findOwnedBy(owner string) ([]dbArchive, error) {
	rows, err := repository.db.Query("SELECT id, owner, name FROM archive WHERE owner = ?", owner)
	if err != nil {
		return nil, fmt.Errorf("query archives: %w", err)
	}
	defer rows.Close()

	var result []dbArchive
	for rows.Next() {
		archive := dbArchive{}

		err = rows.Scan(
			&archive.ID,
			&archive.Owner,
			&archive.Name,
		)
		if err != nil {
			return nil, fmt.Errorf("scan archive row: %w", err)
		}

		result = append(result, archive)
	}

	return result, nil
}

func (repository *sqlArchiveRepository) create(archive dbArchive) (dbArchive, error) {
	result, err := repository.db.Exec(
		"INSERT INTO archive (type,owner,name) VALUES (?, ?, ?)",
		filesarchive.TYPE, archive.Owner, archive.Name,
	)
	if err != nil {
		return dbArchive{}, fmt.Errorf("insert archive to DB: %w", err)
	}

	newInstanceID, err := result.LastInsertId()
	if err != nil {
		return dbArchive{}, fmt.Errorf("get new instance id: %w", err)
	}

	return repository.get(newInstanceID)
}
