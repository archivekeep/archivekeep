package application

import (
	"database/sql"
	"fmt"

	"github.com/google/uuid"
)

type Session struct {
	ID     int64
	Token  string
	UserID int64
}

type SessionRepository struct {
	db *sql.DB
}

func (r *SessionRepository) Create(userID int64) (string, error) {
	token, err := uuid.NewRandom()
	if err != nil {
		return "", fmt.Errorf("generate token from UUID: %e", err)
	}

	_, err = r.db.Exec("INSERT INTO session (token, user_id) VALUES (?, ?)", token.String(), userID)
	if err != nil {
		return "", fmt.Errorf("storing session: %w", err)
	}

	return token.String(), nil
}

func (r *SessionRepository) GetSessionByToken(token string) (*Session, error) {
	rows, err := r.db.Query("SELECT id, user_id FROM session WHERE token = ?", token)
	if err != nil {
		return nil, fmt.Errorf("query sessions: %w", err)
	}
	defer rows.Close()

	if rows.Next() {
		session := Session{}

		err = rows.Scan(
			&session.ID,
			&session.UserID,
		)
		if err != nil {
			return nil, fmt.Errorf("scan session row: %w", err)
		}

		return &session, nil
	}

	return nil, nil
}
