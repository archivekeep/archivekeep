package application

import "errors"

var (
	errDbNotExist = errors.New("instance not exists in DB")
)
