package spa

import (
	"fmt"
	"regexp"
)

const (
	publicPathPlaceholder = "__PUBLIC_PATH_PLACEHOLDER__"
)

var (
	publicPathReplaceRegex = regexp.MustCompile(fmt.Sprintf("/?%s/?", regexp.QuoteMeta(publicPathPlaceholder)))
)
