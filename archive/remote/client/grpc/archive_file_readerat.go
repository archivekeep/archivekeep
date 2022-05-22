package grpcarchive

import (
	"context"
	"fmt"
	"io"

	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
)

type fileReaderAt struct {
	archiveServiceClient pb.ArchiveServiceClient

	fileReferenceName string
}

func (f *fileReaderAt) ReadAt(p []byte, off int64) (n int, err error) {
	client, err := f.archiveServiceClient.DownloadArchiveFile(context.TODO(), &pb.DownloadArchiveFileRequest{
		Name:     f.fileReferenceName,
		SkipHead: true,

		Offset: off,
		Limit:  int64(len(p)),
	})

	if err != nil {
		return 0, err
	}
	defer client.CloseSend()

	for nBytesRead := 0; nBytesRead < len(p); {
		c, err := client.Recv()
		if err != nil {
			if err == io.EOF {
				return nBytesRead, nil
			}

			return nBytesRead, fmt.Errorf("receive stream message: %w", err)
		}

		chunk := c.GetChunk()
		if chunk == nil {
			return nBytesRead, fmt.Errorf("non-first message isn't chunk")
		}

		chunkBytes := chunk.Chunk

		sliceCopySize := len(chunkBytes)
		if nBytesRead+sliceCopySize > len(p) {
			sliceCopySize = len(p) - nBytesRead
		}

		copy(p[nBytesRead:nBytesRead+sliceCopySize], chunkBytes[:])
		nBytesRead += sliceCopySize
	}

	return len(p), nil
}

func (f *fileReaderAt) Close() error {
	return nil
}

func newFileReaderAt(
	archiveServiceClient pb.ArchiveServiceClient,
	fileReferenceName string,
) *fileReaderAt {
	return &fileReaderAt{
		archiveServiceClient: archiveServiceClient,
		fileReferenceName:    fileReferenceName,
	}
}
