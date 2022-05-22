package ioutil

import "io"

type ReaderAtFromSeeker struct {
	rsc io.ReadSeekCloser
}

var (
	_ io.Closer   = ReaderAtFromSeeker{}
	_ io.ReaderAt = ReaderAtFromSeeker{}
)

func NewReaderAtFromSeeker(rsc io.ReadSeekCloser) ReaderAtFromSeeker {
	return ReaderAtFromSeeker{rsc}
}

func (r ReaderAtFromSeeker) Close() error {
	return r.rsc.Close()
}

func (r ReaderAtFromSeeker) ReadAt(p []byte, off int64) (int, error) {
	if _, err := r.rsc.Seek(off, io.SeekStart); err != nil {
		return 0, err
	}
	return r.rsc.Read(p)
}
