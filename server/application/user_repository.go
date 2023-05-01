package application

import (
	"database/sql"
	"fmt"
)

type User struct {
	ID       int64
	Email    string
	Password string
}

func (u User) ResourceName() string {
	return fmt.Sprintf("users/%d", u.ID)
}

func (u User) ByEmailResourceName() string {
	return fmt.Sprintf("users-by-email/%s", u.Email)
}

type UserRepository struct {
	db *sql.DB
}

func (r *UserRepository) GetUserByUserID(id int64) (*User, error) {
	rows, err := r.db.Query("SELECT id, email, password FROM user WHERE id = ?", id)
	if err != nil {
		return nil, fmt.Errorf("query users: %w", err)
	}
	defer rows.Close()

	if rows.Next() {
		user := User{}

		err = rows.Scan(
			&user.ID,
			&user.Email,
			&user.Password,
		)
		if err != nil {
			return nil, fmt.Errorf("scan user row: %w", err)
		}

		return &user, nil
	}

	return nil, nil
}

func (r *UserRepository) GetUserByUserName(username string) (*User, error) {
	rows, err := r.db.Query("SELECT id, email, password FROM user WHERE email = ?", username)
	if err != nil {
		return nil, fmt.Errorf("query users: %w", err)
	}
	defer rows.Close()

	if rows.Next() {
		user := User{}

		err = rows.Scan(
			&user.ID,
			&user.Email,
			&user.Password,
		)
		if err != nil {
			return nil, fmt.Errorf("scan user row: %w", err)
		}

		return &user, nil
	}

	return nil, errDbNotExist
}

func (r *UserRepository) CreateUser(user User) (User, error) {
	result, err := r.db.Exec("INSERT INTO user (email, password) VALUES (?,?)", user.Email, user.Password)
	if err != nil {
		return User{}, fmt.Errorf("exec insert: %w", err)
	}

	user.ID, err = result.LastInsertId()
	if err != nil {
		return User{}, fmt.Errorf("get last insert ID: %w", err)
	}

	return user, nil
}
