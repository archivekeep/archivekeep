package archive

import "io"

type FileReadAtCloser interface {
	io.ReaderAt
	io.Closer
}

type FileInfo struct {
	// Absolute path of file from archive root
	Path string

	Length int64
	Digest map[string]string
}

type Reader interface {
	ListFiles() ([]*FileInfo, error)

	OpenFile(filePath string) (FileInfo, io.ReadCloser, error)
	OpenSeekableFile(filePath string) (FileInfo, io.ReadSeekCloser, error)
	OpenReaderAtFile(filePath string) (FileInfo, FileReadAtCloser, error)
}

type Writer interface {
	SaveFile(reader io.Reader, fileInfo *FileInfo) error
	MoveFile(from string, to string) error

	DeleteFile(filename string) error
}

type ReadWriter interface {
	Reader
	Writer
}
