package application

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"

	"github.com/google/uuid"
	"golang.org/x/crypto/pbkdf2"

	"github.com/archivekeep/archivekeep/server/api"
)

type personalAccessTokenApplicationService struct {
	personalAccessTokenRepository *PersonalAccessTokenRepository
	userRepository                *UserRepository
}

func (p *personalAccessTokenApplicationService) CreatePersonalAccessToken(
	ctx context.Context,
	request *api.CreatePersonalAccessTokenRequest,
) (*api.PersonalAccessToken, error) {
	userID, loggedIn := getLoggedInUserID(ctx)
	if !loggedIn {
		return nil, api.ErrNotAuthorized
	}

	tokenSalt, err := generateTokenSalt()
	if err != nil {
		return nil, fmt.Errorf("generate salt: %w", err)
	}

	tokenUUID, err := uuid.NewRandom()
	if err != nil {
		return nil, fmt.Errorf("generate token: %w", err)
	}
	token := tokenUUID.String()

	tokenHash := hashToken(token, tokenSalt)

	dbToken := PersonalAccessToken{
		UserID:         userID,
		Name:           request.Name,
		TokenSalt:      tokenSalt,
		TokenHash:      tokenHash,
		Token:          token,
		TokenLastEight: token[len(token)-8:],
	}

	dbToken, err = p.personalAccessTokenRepository.CreatePersonalAccessToken(dbToken)
	if err != nil {
		return nil, fmt.Errorf("store token: %w", err)
	}

	return &api.PersonalAccessToken{
		ID:             dbToken.Token,
		Name:           dbToken.Name,
		Token:          dbToken.Token,
		TokenLastEight: dbToken.TokenLastEight,
	}, nil
}

func (p *personalAccessTokenApplicationService) TryTokenAuth(_ context.Context, token string) (*User, error) {
	if len(token) < 8 {
		return nil, fmt.Errorf("too short token: %v", token)
	}

	tokens, err := p.personalAccessTokenRepository.FindPersonalAccessTokensByLastEight(token[len(token)-8:])
	if err != nil {
		return nil, fmt.Errorf("query tokens: %w", err)
	}

	var matchedToken *PersonalAccessToken

	for idx, dbToken := range tokens {
		hash := hashToken(token, dbToken.TokenSalt)
		if hash == dbToken.TokenHash {
			matchedToken = &tokens[idx]
		}
	}

	if matchedToken == nil {
		return nil, errDbNotExist
	}

	user, err := p.userRepository.GetUserByUserID(matchedToken.UserID)
	if err != nil {
		return nil, fmt.Errorf("get user %d for token: %w", matchedToken.UserID, err)
	}

	return user, nil
}

func hashToken(token string, salt string) string {
	hashBytes := pbkdf2.Key([]byte(token), []byte(salt), 8_000, 60, sha256.New)

	return base64.StdEncoding.EncodeToString(hashBytes)
}

func generateTokenSalt() (string, error) {
	var saltBytes [30]byte

	_, err := rand.Read(saltBytes[:])
	if err != nil {
		return "", err
	}

	return base64.StdEncoding.EncodeToString(saltBytes[:])[0:30], nil
}
