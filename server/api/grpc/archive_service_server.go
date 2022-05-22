package grpc_ak

import (
	"context"
	"fmt"
	"io"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/archivekeep/archivekeep/archive"
	pb "github.com/archivekeep/archivekeep/internal/grpc/protobuf"
	"github.com/archivekeep/archivekeep/server/api"
)

const chunkSize = 64 * 1024 // 64 KiB

type archiveServiceServer struct {
	pb.UnimplementedArchiveServiceServer

	archiveService api.ArchiveService
}

func NewArchiveServiceServer(archiveProvider api.ArchiveService) pb.ArchiveServiceServer {
	return &archiveServiceServer{
		archiveService: archiveProvider,
	}
}

func (s *archiveServiceServer) GetArchive(ctx context.Context, request *pb.GetArchiveRequest) (*pb.GetArchiveResponse, error) {
	archiveName, err := ArchiveResourceName(request.Name).DeconstructParts()
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "deconstruct name: %v", err)
	}

	_, err = s.archiveService.GetArchive(ctx, archiveName)
	if err != nil {
		return nil, fmt.Errorf("get archive with name %s: %w", archiveName, err)
	}

	return &pb.GetArchiveResponse{
		Archive: &pb.Archive{
			Name: archiveName,
		},
	}, nil
}

func (s *archiveServiceServer) ListArchiveFiles(
	ctx context.Context,
	request *pb.ListArchiveFilesRequest,
) (*pb.ListArchiveFilesResponse, error) {
	archiveName, err := ArchiveResourceName(request.Parent).DeconstructParts()
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "deconstruct name: %v", err)
	}

	a, err := s.archiveService.GetArchiveAccessor(ctx, archiveName)
	if err != nil {
		return nil, fmt.Errorf("get archive with name %s: %w", archiveName, err)
	}
	if a == nil {
		return nil, fmt.Errorf("get archive with name %s: %w", archiveName, api.ErrNotFound)
	}

	archiveReader, err := a.OpenReader()
	if err != nil {
		return nil, status.Errorf(codes.Internal, "open archive for reading: %v", err)
	}

	files, err := archiveReader.ListFiles()
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "get stored files: %v", err)
	}

	pbFiles := make([]*pb.ArchiveFile, 0, len(files))
	for _, file := range files {
		pbFiles = append(pbFiles, &pb.ArchiveFile{
			Name:    fmt.Sprintf("%s/files/%s", request.Parent, file.Path),
			Length:  file.Length,
			Digests: file.Digest,
		})
	}

	return &pb.ListArchiveFilesResponse{
		Files: pbFiles,
	}, nil
}

func (s *archiveServiceServer) UploadArchiveFile(streamServer pb.ArchiveService_UploadArchiveFileServer) error {
	msg, err := streamServer.Recv()
	if err != nil {
		return err
	}
	head := msg.GetHead()
	if head == nil {
		return fmt.Errorf("first message isn't head")
	}

	archiveName, filePath, err := ArchiveFileResourceName(head.Name).DeconstructParts()
	if err != nil {
		return status.Errorf(codes.InvalidArgument, "deconstruct name: %v", err)
	}

	a, err := s.archiveService.GetArchiveAccessor(streamServer.Context(), archiveName)
	if err != nil {
		return fmt.Errorf("get archive with name %s: %w", archiveName, err)
	}
	if a == nil {
		return fmt.Errorf("get archive with name %s: %w", archiveName, api.ErrNotFound)
	}

	archiveWriter, err := a.OpenWriter()
	if err != nil {
		return status.Errorf(codes.Internal, "open archive for writing: %v", err)
	}

	pipeReader, pipeWriter := io.Pipe()

	go func() {
		streamReadErr := func() error {
			for {
				c, err := streamServer.Recv()
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

	err = archiveWriter.SaveFile(pipeReader, &archive.FileInfo{
		Path:   filePath,
		Length: head.Length,
		Digest: head.Digests,
	})
	if err != nil {
		return fmt.Errorf("save file: %w", err)
	}

	err = streamServer.SendAndClose(&pb.ArchiveFile{
		Name:    head.Name,
		Digests: head.Digests,
	})
	if err != nil {
		return fmt.Errorf("send and close: %w", err)
	}
	return nil
}

func (s *archiveServiceServer) MoveArchiveFile(ctx context.Context, request *pb.MoveArchiveFileRequest) (*pb.ArchiveFile, error) {
	srcArchiveName, srcFilePath, err := ArchiveFileResourceName(request.Name).DeconstructParts()
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "deconstruct name: %v", err)
	}

	dstArchiveName, dstFilePath, err := ArchiveFileResourceName(request.DestinationName).DeconstructParts()
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "deconstruct destination name: %v", err)
	}

	if srcArchiveName != dstArchiveName {
		return nil, status.Errorf(codes.InvalidArgument, "source and destination archive is not the same")
	}

	a, err := s.archiveService.GetArchiveAccessor(ctx, srcArchiveName)
	if err != nil {
		return nil, fmt.Errorf("get archive with name %s: %w", srcArchiveName, err)
	}
	if a == nil {
		return nil, fmt.Errorf("get archive with name %s: %w", srcArchiveName, api.ErrNotFound)
	}

	archiveWriter, err := a.OpenWriter()
	if err != nil {
		return nil, status.Errorf(codes.Internal, "open archive for writing: %v", err)
	}

	err = archiveWriter.MoveFile(srcFilePath, dstFilePath)
	if err != nil {
		return nil, err
	}

	return &pb.ArchiveFile{
		Name:    request.DestinationName,
		Digests: nil, // TODO: populate digests
	}, nil
}

func (s *archiveServiceServer) DownloadArchiveFile(
	request *pb.DownloadArchiveFileRequest,
	streamServer pb.ArchiveService_DownloadArchiveFileServer,
) error {
	archiveName, filePath, err := ArchiveFileResourceName(request.Name).DeconstructParts()
	if err != nil {
		return status.Errorf(codes.InvalidArgument, "deconstruct name: %v", err)
	}

	a, err := s.archiveService.GetArchiveAccessor(streamServer.Context(), archiveName)
	if err != nil {
		return fmt.Errorf("get archive with name %s: %w", archiveName, err)
	}
	if a == nil {
		return fmt.Errorf("get archive with name %s: %w", archiveName, api.ErrNotFound)
	}

	archiveReader, err := a.OpenReader()
	if err != nil {
		return status.Errorf(codes.Internal, "open archive for reading: %v", err)
	}

	info, dataReadCloser, err := archiveReader.OpenFile(filePath)
	if err != nil {
		return status.Errorf(codes.Internal, "couldn't open file: %v", filePath)
	}
	defer dataReadCloser.Close()

	response := &pb.DownloadArchiveFileResponse{}
	if !request.SkipHead {
		response.Value = &pb.DownloadArchiveFileResponse_Head{
			Head: &pb.ArchiveFile{
				Name:    request.Name,
				Length:  info.Length,
				Digests: info.Digest,
			},
		}
		if err := streamServer.Send(response); err != nil {
			return status.Errorf(codes.Internal, "couldn't send head")
		}
	}

	if request.Offset > 0 {
		// TODO: optimize, use seeking
		_, err := io.CopyN(io.Discard, dataReadCloser, request.Offset)
		if err != nil {
			return status.Errorf(codes.Internal, "couldn't seek: %v", err)
		}
	}

	end := info.Length
	if request.Limit > 0 {
		end = request.Offset + request.Limit
	}

	buf := make([]byte, chunkSize)
	for currentByte := request.Offset; currentByte < end; {

		currentChunkSize, err := dataReadCloser.Read(buf[:])
		if err != nil {
			return status.Errorf(codes.Internal, "couldn't read file: %v", err)
		}

		response.Value = &pb.DownloadArchiveFileResponse_Chunk{
			Chunk: &pb.BytesChunk{
				Chunk: buf[0:currentChunkSize],
			},
		}

		if err := streamServer.Send(response); err != nil {
			return status.Errorf(codes.Internal, "couldn't send chunk")
		}

		currentByte += int64(currentChunkSize)
	}

	return nil
}
