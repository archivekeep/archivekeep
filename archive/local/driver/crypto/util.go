package cryptoarchive

import "io"

type readSeekerWithSeparateCloser struct {
	io.ReadSeeker
	io.Closer
}
