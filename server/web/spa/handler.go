package spa

import (
	"net/http"
)

var HandlerFuncFactory func(config Config) http.HandlerFunc
