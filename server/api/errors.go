package api

import "errors"

var (
	// ErrNotAuthorized should be returned if user can see resource, but can't perform the operation.
	//
	// Be cautious to not return this error for invocations, where user can't access a resource all. In such cases
	// ErrNotFound might be more appropriate to prevent information leak informing, that resource exists, but user
	// can't access it.
	//
	// TODO: API implementation to provide option.rewriteNotAuthorized to "not found or not authorized" for all
	// not authorized and not found errors to signal user authorization might be needed but won't leak information.
	ErrNotAuthorized = errors.New("not authorized")

	ErrNotFound       = errors.New("not found")
	ErrNotImplemented = errors.New("not implemented")
)
