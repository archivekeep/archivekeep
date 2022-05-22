package application

import (
	"context"
	"fmt"

	"golang.org/x/crypto/bcrypt"
)

type UserService struct {
	repository *UserRepository
}

func (s *UserService) VerifyLogin(context context.Context, username string, password string) (*User, error) {
	user, err := s.repository.GetUserByUserName(username)
	if err != nil {
		return nil, fmt.Errorf("get user: %w", err)
	}
	if user == nil {
		return nil, fmt.Errorf("user not found")
	}

	err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password))
	if err != nil {
		return nil, fmt.Errorf("verify password: %w", err)
	}

	return user, nil
}

func (s *UserService) CreateUser(emailAddress string, password string) error {
	existingUser, err := s.repository.GetUserByUserName(emailAddress)
	if err != nil {
		return fmt.Errorf("check for existing user: %w", err)
	}
	if existingUser != nil {
		return fmt.Errorf("user already exists")
	}

	passwordHash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost+1)
	if err != nil {
		return fmt.Errorf("generate bcrypt hash: %w", err)
	}

	return s.repository.CreateUser(User{
		Email:    emailAddress,
		Password: string(passwordHash),
	})
}
