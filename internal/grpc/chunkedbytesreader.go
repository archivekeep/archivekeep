package grpc

import (
	"io"
)

func NewChunkedBytesReader() io.Reader {
	pipeReader, pipeWriter := io.Pipe()

	pipeWriter.Close()

	return pipeReader
}
