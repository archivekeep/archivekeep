package application

import (
	"database/sql"
	"fmt"
	"strconv"

	"github.com/archivekeep/archivekeep/server/api"
)

type dbArchivePermission struct {
	ArchiveID int64
	ID        int64

	SubjectName string
}

func (s dbArchivePermission) toAPIDTO() api.ArchivePermissionsDetails {
	return api.ArchivePermissionsDetails{
		ArchiveID:    fmt.Sprintf("%d", s.ArchiveID),
		PermissionID: fmt.Sprintf("%d", s.ID),

		SubjectName: s.SubjectName,
	}
}

type sqlArchivePermissionRepository struct {
	db *sql.DB
}

func (repository *sqlArchivePermissionRepository) findForArchive(archiveIDStr string) ([]dbArchivePermission, error) {
	archiveID, err := strconv.ParseInt(archiveIDStr, 10, 64)
	if err != nil {
		return []dbArchivePermission{}, fmt.Errorf("ID parsing failed: %w", err)
	}

	rows, err := repository.db.Query("SELECT archive_id, id, subject_name FROM archive_permission WHERE archive_id = ?", archiveID)
	if err != nil {
		return nil, fmt.Errorf("query archives: %w", err)
	}
	defer rows.Close()

	var result []dbArchivePermission
	for rows.Next() {
		archive := dbArchivePermission{}

		err = rows.Scan(
			&archive.ArchiveID,
			&archive.ID,
			&archive.SubjectName,
		)
		if err != nil {
			return nil, fmt.Errorf("scan archive row: %w", err)
		}

		result = append(result, archive)
	}

	return result, nil
}

func (repository *sqlArchivePermissionRepository) get(idStr string) (dbArchivePermission, error) {
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return dbArchivePermission{}, fmt.Errorf("ID parsing failed: %w", err)
	}

	return repository.getByIntId(id)
}

func (repository *sqlArchivePermissionRepository) getByIntId(id int64) (dbArchivePermission, error) {
	rows, err := repository.db.Query("SELECT archive_id, id, subject_name FROM archive_permission WHERE id = ?", id)
	if err != nil {
		return dbArchivePermission{}, fmt.Errorf("query archives: %w", err)
	}
	defer rows.Close()

	if !rows.Next() {
		return dbArchivePermission{}, errDbNotExist
	}

	archivePermission := dbArchivePermission{}

	err = rows.Scan(
		&archivePermission.ArchiveID,
		&archivePermission.ID,
		&archivePermission.SubjectName,
	)
	if err != nil {
		return dbArchivePermission{}, fmt.Errorf("scan archive row: %w", err)
	}

	return archivePermission, nil
}

func (repository *sqlArchivePermissionRepository) delete(id int64) error {
	_, err := repository.db.Exec("DELETE FROM archive_permission WHERE id = ?", id)

	return err
}

func (repository *sqlArchivePermissionRepository) create(archive dbArchivePermission) (dbArchivePermission, error) {
	result, err := repository.db.Exec(
		"INSERT INTO archive_permission (archive_id, subject_name) VALUES (?, ?)",
		archive.ArchiveID,
		archive.SubjectName,
	)
	if err != nil {
		return dbArchivePermission{}, fmt.Errorf("insert archive to DB: %w", err)
	}

	newInstanceID, err := result.LastInsertId()
	if err != nil {
		return dbArchivePermission{}, fmt.Errorf("get new instance id: %w", err)
	}

	return repository.getByIntId(newInstanceID)
}
