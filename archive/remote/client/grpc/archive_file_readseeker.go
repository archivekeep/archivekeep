package grpcarchive

import (
	"context"
	"fmt"
	"io"

	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
)

type fileReadSeeker struct {
	archiveServiceClient pb.ArchiveServiceClient

	fileReferenceName string

	pipeReader *io.PipeReader
}

func (f *fileReadSeeker) Read(data []byte) (n int, err error) {
	return f.pipeReader.Read(data)
}

func (f *fileReadSeeker) Seek(offset int64, whence int) (int64, error) {
	if whence != io.SeekStart {
		return -1, fmt.Errorf("only io.SeekStart is supported")
	}

	err := f.pipeReader.Close()
	if err != nil {
		return 0, fmt.Errorf("close existing for seek: %w", err)
	}

	client, err := f.archiveServiceClient.DownloadArchiveFile(context.TODO(), &pb.DownloadArchiveFileRequest{
		Name:     f.fileReferenceName,
		SkipHead: true,
		Offset:   offset,
	})
	if err != nil {
		return 0, err
	}

	f.start(client)

	return offset, nil
}

func (f *fileReadSeeker) Close() error {
	return f.pipeReader.Close()
}

func (f *fileReadSeeker) start(downloadClient pb.ArchiveService_DownloadArchiveFileClient) {
	pipeReader, pipeWriter := io.Pipe()

	go func() {
		defer downloadClient.CloseSend()

		streamReadErr := func() error {
			for {
				c, err := downloadClient.Recv()
				if err != nil {
					if err == io.EOF {
						return nil
					}

					return fmt.Errorf("receive stream message: %w", err)
				}

				chunk := c.GetChunk()
				if chunk == nil {
					return fmt.Errorf("non-first message isn't chunk")
				}

				_, err = pipeWriter.Write(chunk.Chunk)
				if err != nil {
					return fmt.Errorf("write to pipe: %w", err)
				}
			}
		}()

		pipeWriter.CloseWithError(streamReadErr)
	}()

	f.pipeReader = pipeReader
}

func newFileReadSeeker(
	archiveServiceClient pb.ArchiveServiceClient,
	fileReferenceName string,
	downloadClient pb.ArchiveService_DownloadArchiveFileClient,
	head *pb.ArchiveFile,
) *fileReadSeeker {
	frs := &fileReadSeeker{
		archiveServiceClient: archiveServiceClient,
		fileReferenceName:    fileReferenceName,
	}

	frs.start(downloadClient)

	return frs
}
