package grpcarchive

import (
	"context"
	"fmt"
	"io"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/archivekeep/archivekeep/archive"
	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	grpc_ak "github.com/archivekeep/archivekeep/server/api/grpc"
)

const chunkSize = 64 * 1024 // 64 KiB

type RemoteArchive struct {
	client      pb.ArchiveServiceClient
	archiveName string
}

func NewRemoteArchive(
	conn grpc.ClientConnInterface,
	archiveName string,
) *RemoteArchive {
	client := pb.NewArchiveServiceClient(conn)

	return &RemoteArchive{
		client:      client,
		archiveName: archiveName,
	}
}

func (r *RemoteArchive) ListFiles() ([]*archive.FileInfo, error) {
	response, err := r.client.ListArchiveFiles(context.TODO(), &pb.ListArchiveFilesRequest{
		Parent: fmt.Sprintf("archives/%s", r.archiveName),
	})
	if err != nil {
		return nil, err
	}

	files := make([]*archive.FileInfo, 0, len(response.Files))
	for _, file := range response.Files {
		_, fileName, _ := grpc_ak.ArchiveFileResourceName(file.Name).DeconstructParts()

		files = append(files, &archive.FileInfo{
			Path:   fileName,
			Length: file.Length,
			Digest: file.Digests,
		})
	}

	return files, nil
}

func (r *RemoteArchive) OpenFile(filename string) (archive.FileInfo, io.ReadCloser, error) {
	return r.OpenSeekableFile(filename)
}

func (r *RemoteArchive) OpenSeekableFile(filename string) (archive.FileInfo, io.ReadSeekCloser, error) {
	client, err := r.client.DownloadArchiveFile(context.TODO(), &pb.DownloadArchiveFileRequest{
		Name: r.fileReferenceName(filename),
	})
	if err != nil {
		return archive.FileInfo{}, nil, err
	}

	msg, err := client.Recv()
	if err != nil {
		return archive.FileInfo{}, nil, fmt.Errorf("receive head: %w", err)
	}
	head := msg.GetHead()
	if head == nil {
		return archive.FileInfo{}, nil, fmt.Errorf("first message isn't head")
	}

	readSeekCloser := newFileReadSeeker(
		r.client,
		r.fileReferenceName(filename),
		client,
		head,
	)

	return headToFileInfo(head), readSeekCloser, nil
}

func (r *RemoteArchive) OpenReaderAtFile(filename string) (archive.FileInfo, archive.FileReadAtCloser, error) {
	client, err := r.client.DownloadArchiveFile(context.TODO(), &pb.DownloadArchiveFileRequest{
		Name: r.fileReferenceName(filename),
	})
	if err != nil {
		return archive.FileInfo{}, nil, err
	}
	defer client.CloseSend()

	msg, err := client.Recv()
	if err != nil {
		return archive.FileInfo{}, nil, fmt.Errorf("receive head: %w", err)
	}
	head := msg.GetHead()
	if head == nil {
		return archive.FileInfo{}, nil, fmt.Errorf("first message isn't head")
	}

	readerAt := newFileReaderAt(
		r.client,
		r.fileReferenceName(filename),
	)

	return headToFileInfo(head), readerAt, nil
}

func (r *RemoteArchive) SaveFile(reader io.Reader, fileInfo *archive.FileInfo) error {
	streamClient, err := r.client.UploadArchiveFile(context.TODO())
	if err != nil {
		return fmt.Errorf("start stream: %w", err)
	}

	err = streamClient.Send(&pb.UploadArchiveFileRequest{
		Value: &pb.UploadArchiveFileRequest_Head{
			Head: &pb.ArchiveFile{
				Name:    r.fileReferenceName(fileInfo.Path),
				Length:  fileInfo.Length,
				Digests: fileInfo.Digest,
			},
		},
	})
	if err != nil {
		return fmt.Errorf("send head: %w", err)
	}

	message := &pb.UploadArchiveFileRequest{}
	buf := make([]byte, chunkSize)
	for currentByte := 0; int64(currentByte) < fileInfo.Length; {
		currentChunkSize, err := reader.Read(buf[:])
		if err != nil {
			return status.Errorf(codes.Internal, "read chunk to send: %v", err)
		}

		message.Value = &pb.UploadArchiveFileRequest_Chunk{
			Chunk: &pb.BytesChunk{
				Chunk: buf[0:currentChunkSize],
			},
		}

		if err := streamClient.Send(message); err != nil {
			return status.Errorf(codes.Internal, "couldn't send chunk: %v", err)
		}

		currentByte += currentChunkSize
	}

	_, err = streamClient.CloseAndRecv()
	if err != nil {
		return fmt.Errorf("close and receive result: %w", err)
	}

	return nil
}

func (r *RemoteArchive) MoveFile(
	from string,
	to string,
) error {
	_, err := r.client.MoveArchiveFile(context.TODO(), &pb.MoveArchiveFileRequest{
		Name:            r.fileReferenceName(from),
		DestinationName: r.fileReferenceName(to),
	})

	return err
}

func (r *RemoteArchive) DeleteFile(filename string) error {
	return fmt.Errorf("not implemented")
}

func (r *RemoteArchive) fileReferenceName(from string) string {
	return fmt.Sprintf("archives/%s/files/%s", r.archiveName, from)
}

func headToFileInfo(head *pb.ArchiveFile) archive.FileInfo {
	_, fileName, _ := grpc_ak.ArchiveFileResourceName(head.Name).DeconstructParts()

	return archive.FileInfo{
		Path:   fileName,
		Length: head.Length,
		Digest: head.Digests,
	}
}
