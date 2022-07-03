package application

import (
	"database/sql"
	"fmt"
)

type PersonalAccessToken struct {
	ID             int64
	UserID         int64
	Name           string
	TokenSalt      string
	TokenHash      string
	Token          string
	TokenLastEight string
}

type PersonalAccessTokenRepository struct {
	db *sql.DB
}

func (r *PersonalAccessTokenRepository) FindPersonalAccessTokensByLastEight(
	tokenLastEight string,
) ([]PersonalAccessToken, error) {
	rows, err := r.db.Query(
		"SELECT id, user_id, name, token_salt, token_hash, token_last_eight FROM personal_access_token WHERE token_last_eight = ?",
		tokenLastEight,
	)
	if err != nil {
		return nil, fmt.Errorf("query personal_access_token: %w", err)
	}
	defer rows.Close()

	var tokens []PersonalAccessToken
	if rows.Next() {
		token := PersonalAccessToken{}

		err = rows.Scan(
			&token.ID,
			&token.UserID,
			&token.Name,
			&token.TokenSalt,
			&token.TokenHash,
			&token.TokenLastEight,
		)
		if err != nil {
			return nil, fmt.Errorf("scan PersonalAccessToken row: %w", err)
		}

		tokens = append(tokens, token)
	}

	return tokens, nil
}

func (r *PersonalAccessTokenRepository) CreatePersonalAccessToken(token PersonalAccessToken) (PersonalAccessToken, error) {
	result, err := r.db.Exec(
		"INSERT INTO personal_access_token (user_id, name, token_salt, token_hash, token_last_eight) VALUES (?,?,?,?,?)",
		token.UserID,
		token.Name,
		token.TokenSalt,
		token.TokenHash,
		token.TokenLastEight,
	)
	if err != nil {
		return PersonalAccessToken{}, fmt.Errorf("exec insert: %w", err)
	}

	saved := token

	saved.ID, err = result.LastInsertId()
	if err != nil {
		return PersonalAccessToken{}, fmt.Errorf("get last insert ID: %w", err)
	}

	return saved, nil
}
