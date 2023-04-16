package filesarchive

import (
	"bytes"
	"fmt"
	"io"
	"io/fs"
	"log"
	"os"
	paths "path"
	"regexp"
	"sort"
	"strings"
	"sync"

	"github.com/spf13/afero"

	archiveapi "github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/internal/util/cwalk"
)

const (
	ignoreFileName = ".archivekeepignore"
)

type Archive struct {
	rootFs afero.Afero
}

func Init(fs afero.Fs, path string) (*Archive, error) {
	checksumDir := paths.Join(path, ".archive", "checksums")

	checksumExists, err := afero.Exists(fs, checksumDir)
	if err != nil {
		return nil, fmt.Errorf("check if file exists: %w", err)
	}
	if checksumExists {
		return nil, fmt.Errorf("archive already exists")
	}

	err = os.MkdirAll(checksumDir, 0755)
	if err != nil {
		return nil, fmt.Errorf("create checksums dir: %w", err)
	}

	return Open(fs, path)
}

func Open(fs afero.Fs, path string) (*Archive, error) {
	checksumDir := paths.Join(path, ".archive", "checksums")

	checksumExists, err := afero.Exists(fs, checksumDir)
	if err != nil {
		return nil, fmt.Errorf("check if file exists: %w", err)
	}
	if !checksumExists {
		return nil, fmt.Errorf("archive does not exist")
	}

	if path != "." {
		fs = afero.NewBasePathFs(fs, path)
	}

	return &Archive{
		rootFs: afero.Afero{Fs: fs},
	}, nil
}

func (archive *Archive) Add(path string) error {
	sum, err := util.ComputeChecksum(archive.rootFs, path)
	if err != nil {
		return fmt.Errorf("compute checksum: %w", err)
	}

	return archive.storeFileChecksum(path, sum)
}

func (archive *Archive) Remove(path string) error {
	return archive.rootFs.Remove(archive.checksumPath(path))
}

func (archive *Archive) Contains(path string) bool {
	if exists, _ := archive.rootFs.Exists(archive.checksumPath(path)); exists {
		return true
	}
	return false
}

func (archive *Archive) VerifyFileExists(path string) (bool, error) {
	exists, err := archive.rootFs.Exists(path)
	if err != nil {
		return false, fmt.Errorf("check if file exists: %w", err)
	}

	return exists, nil
}

func (archive *Archive) VerifyFileIntegrity(path string) error {
	exists, err := archive.rootFs.Exists(path)
	if err != nil {
		return fmt.Errorf("check if file exists: %w", err)
	}
	if !exists {
		return fmt.Errorf("file was deleted")
	}

	sum, err := archive.ComputeFileChecksum(path)
	if err != nil {
		return fmt.Errorf("compute checksum: %w", err)
	}

	storedSum, err := archive.FileChecksum(path)
	if err != nil {
		return fmt.Errorf("read stored checksum: %w", err)
	}

	if storedSum != sum {
		return fmt.Errorf("file was modified")
	}

	return nil
}

func (archive *Archive) ComputeFileChecksum(path string) (string, error) {
	return util.ComputeChecksum(archive.rootFs, path)
}

func (archive *Archive) StoredFiles() ([]string, error) {
	basepath := archive.checksumsDirectoryPath()

	var storedFiles []string
	storedFilesChan := make(chan string, 128)
	filesToCollect := &sync.WaitGroup{}
	go func() {
		for stat := range storedFilesChan {
			storedFiles = append(storedFiles, stat)
			filesToCollect.Done()
		}
	}()

	err := cwalk.Walk(archive.rootFs, basepath, func(path string, d fs.FileInfo, err error) error {
		if err != nil {
			fmt.Printf("ERROR: file: %v", err)
			return err
		}

		if strings.HasSuffix(path, ".sha256") {
			storedFilePath := path
			storedFilePath = strings.TrimSuffix(storedFilePath, ".sha256")
			storedFilePath = strings.TrimPrefix(storedFilePath, basepath)
			storedFilePath = strings.TrimPrefix(storedFilePath, "/")

			filesToCollect.Add(1)
			storedFilesChan <- storedFilePath
		}

		return nil
	})
	filesToCollect.Wait()
	close(storedFilesChan)

	if err != nil {
		return nil, fmt.Errorf("walk archive directory: %w", err)
	}

	sort.Strings(storedFiles)

	return storedFiles, nil
}

func (archive *Archive) ListFiles() ([]*archiveapi.FileInfo, error) {
	return util.CollectFiles(archive)
}

func (archive *Archive) OpenFile(filename string) (archiveapi.FileInfo, io.ReadCloser, error) {
	return archive.openFile(filename)
}

func (archive *Archive) OpenSeekableFile(filename string) (archiveapi.FileInfo, io.ReadSeekCloser, error) {
	return archive.openFile(filename)
}

func (archive *Archive) OpenReaderAtFile(filename string) (archiveapi.FileInfo, archiveapi.FileReadAtCloser, error) {
	return archive.openFile(filename)
}

func (archive *Archive) openFile(filename string) (archiveapi.FileInfo, afero.File, error) {
	file, err := archive.rootFs.Open(filename)
	if err != nil {
		return archiveapi.FileInfo{}, nil, fmt.Errorf("open file: %w", err)
	}

	fileStat, err := file.Stat()
	if err != nil {
		return archiveapi.FileInfo{}, nil, fmt.Errorf("stat file: %w", err)
	}

	checksum, err := archive.FileChecksum(filename)
	if err != nil {
		return archiveapi.FileInfo{}, nil, fmt.Errorf("checksum file: %w", err)
	}

	return archiveapi.FileInfo{
		Path:   filename,
		Length: fileStat.Size(),
		Digest: map[string]string{
			"SHA256": checksum,
		},
	}, file, nil
}

func (archive *Archive) FileChecksum(filename string) (string, error) {
	hashFileContents, err := archive.rootFs.ReadFile(archive.checksumPath(filename))
	if err != nil {
		return "", err
	}

	return strings.SplitN(string(hashFileContents), " ", 2)[0], nil
}

func (archive *Archive) FileInfo(filename string) (*archiveapi.FileInfo, error) {
	fileInfo, stream, err := archive.OpenFile(filename)
	if err != nil {
		return nil, err
	}
	defer stream.Close()

	return &fileInfo, nil
}

func (archive *Archive) SaveFileFromBytes(content []byte, filename string) error {
	return archive.SaveFile(
		bytes.NewReader(content),
		&archiveapi.FileInfo{
			Path:   filename,
			Length: int64(len(content)),
			Digest: map[string]string{"SHA256": util.ComputeChecksumFromBytes(content)},
		},
	)
}

func (archive *Archive) SaveFile(reader io.Reader, fileInfo *archiveapi.FileInfo) error {
	filename := fileInfo.Path
	checksum := fileInfo.Digest["SHA256"]

	err := archive.rootFs.MkdirAll(paths.Dir(filename), 0755)
	if err != nil {
		return fmt.Errorf("make directory for file: %w", err)
	}

	targetFile, err := archive.rootFs.OpenFile(filename, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0666)
	if err != nil {
		return err
	}

	processFailureError := func(s string, err error) error {
		// TODO: handle interruption signal (currently, this is not being called)
		fmt.Fprintf(os.Stderr, "save failed, removing: %s\n", filename)

		removeErr := archive.rootFs.Remove(filename)
		if removeErr != nil {
			return fmt.Errorf("%s, remove of corrupted file failed: %w", s, removeErr)
		}

		return fmt.Errorf("save file: %s: %w", s, err)
	}

	err = archive.storeFile(targetFile, reader)
	if err != nil {
		return processFailureError("store file", err)
	}

	resultChecksum, err := util.ComputeChecksum(archive.rootFs, filename)
	if err != nil {
		return processFailureError("compute copy checksum", err)
	}
	if resultChecksum != checksum {
		err = fmt.Errorf("copied file has wrong checksum: got=%s, expected=%s", resultChecksum, checksum)
		return processFailureError("copy result", err)
	}

	err = archive.storeFileChecksum(filename, checksum)
	if err != nil {
		return processFailureError("store checksum", err)
	}

	return nil
}

func (archive *Archive) MoveFile(from string, to string) error {
	destinationFileExists, _ := archive.rootFs.Exists(to) // TODO: error check
	if destinationFileExists {
		return fmt.Errorf("destination file already exists: %v", to)
	}

	checksum, err := archive.FileChecksum(from)
	if err != nil {
		return fmt.Errorf("get existing checksum: %w", err)
	}

	err = archive.storeFileChecksum(to, checksum)
	if err != nil {
		return fmt.Errorf("store checksum for move: %w", err)
	}

	err = archive.rootFs.MkdirAll(paths.Dir(to), 0755)
	if err != nil {
		return fmt.Errorf("mkdir for file: %w", err)
	}

	err = archive.rootFs.Rename(from, to)
	if err != nil {
		// TODO: error check
		archive.rootFs.Remove(archive.checksumPath(to))

		return fmt.Errorf("move to new path: %w", err)
	}

	err = archive.rootFs.Remove(archive.checksumPath(from))
	if err != nil {
		return fmt.Errorf("delete old checksum: %w", err)
	}

	return nil
}

func (archive *Archive) DeleteFile(filename string) error {
	err := archive.rootFs.Remove(filename)
	if err != nil {
		return fmt.Errorf("remove file: %w", err)
	}

	err = archive.rootFs.Remove(archive.checksumPath(filename))
	if err != nil {
		return fmt.Errorf("remove checksum: %w", err)
	}

	return nil
}

func (archive *Archive) FindAllFiles(searchGlobs ...string) ([]string, error) {
	ignorePatterns, err := archive.loadIgnorePatterns()
	if err != nil {
		return nil, fmt.Errorf("load ignore patterns (if present): %w", err)
	}

	matchedFilesAbs := util.FindFilesByGlobsIgnore(
		afero.NewIOFS(archive.rootFs),
		func(path string) bool {
			if util.IsPathIgnored(path) {
				return true
			}

			pathParts := strings.Split(path, "/")

			for _, pattern := range ignorePatterns {
				for _, pathPart := range pathParts {
					match, err := paths.Match(pattern, pathPart)
					if err != nil {
						log.Printf("error checking ignore: %v", err)
					}
					if match {
						return true
					}
				}
			}

			return false
		},
		searchGlobs...,
	)

	var matchedFiles []string
	for _, absFile := range matchedFilesAbs {
		matchedFiles = append(matchedFiles, absFile)
	}

	return matchedFiles, nil
}

func (archive *Archive) checksumPath(path string) string {
	return paths.Join(archive.checksumsDirectoryPath(), path+".sha256")
}

func (archive *Archive) checksumsDirectoryPath() string {
	return paths.Join(".archive", "checksums")
}

func (archive *Archive) storeFile(targetFile afero.File, reader io.Reader) error {
	var closeAttempted bool
	defer (func() {
		if !closeAttempted {
			err := targetFile.Close()
			if err != nil {
				_, _ = fmt.Fprintf(os.Stderr, "close failed: %v\n", err)
			}
		}
	})()

	_, err := io.Copy(targetFile, reader)
	if err != nil {
		return fmt.Errorf("copy: %w", err)
	}

	err = targetFile.Sync()
	if err != nil {
		return fmt.Errorf("sync: %w", err)
	}

	err = targetFile.Close()
	closeAttempted = true
	if err != nil {
		return fmt.Errorf("close: %w", err)
	}

	return nil
}

func (archive *Archive) storeFileChecksum(filename string, checksum string) error {
	hashFilePath := archive.checksumPath(filename)

	err := archive.rootFs.MkdirAll(paths.Dir(hashFilePath), 0755)
	if err != nil {
		return fmt.Errorf("make directory for checksum file: %w", err)
	}

	err = archive.rootFs.WriteFile(hashFilePath, []byte(checksum+"  "+filename+"\n"), 0755)
	if err != nil {
		return fmt.Errorf("write checksum file: %w", err)
	}
	return nil
}

func (archive *Archive) loadIgnorePatterns() ([]string, error) {
	ignoreFileExists, err := archive.rootFs.Exists(ignoreFileName)
	if err != nil {
		return nil, fmt.Errorf("check ignore file existence: %w", err)
	}

	if !ignoreFileExists {
		return nil, nil
	}

	ignoreFileBytes, err := archive.rootFs.ReadFile(ignoreFileName)
	if err != nil {
		return nil, fmt.Errorf("read ignore file contents: %w", err)
	}

	lines := regexp.MustCompile("\r?\n").Split(string(ignoreFileBytes), -1)

	var patterns []string
	for _, line := range lines {
		pattern := strings.TrimSpace(line)

		if pattern == "" || pattern[0] == '#' {
			continue
		}

		patterns = append(patterns, pattern)
	}

	return patterns, nil
}
