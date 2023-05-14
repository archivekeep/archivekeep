package cryptoarchive

import (
	"bytes"
	"fmt"
	"io"
	"io/fs"
	"os"
	paths "path"
	"sort"
	"strings"
	"sync"

	"github.com/keybase/saltpack/basic"
	"github.com/spf13/afero"

	archive2 "github.com/archivekeep/archivekeep/archive"
	akcf "github.com/archivekeep/archivekeep/crypto/file"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
	"github.com/archivekeep/archivekeep/internal/util"
	"github.com/archivekeep/archivekeep/internal/util/cwalk"
	"github.com/archivekeep/archivekeep/internal/util/ioutil"
)

const (
	directoryCreationPerm = 0755
	fileCreationPerm      = 0644

	encryptedFileExtension = ".enc"
)

type Archive struct {
	path    string
	keyring *keyring
}

func Create(
	path string,
	passwordProvider func() (string, error),
) (*Archive, error) {
	archive := &Archive{path: path}

	err := archive.create(passwordProvider)
	if err != nil {
		return nil, err
	}

	return archive, nil
}

func Open(
	path string,
	passwordProvider func() (string, error),
) (*Archive, error) {
	archive := &Archive{path: path}

	err := archive.open(passwordProvider)
	if err != nil {
		return nil, err
	}

	return archive, nil
}

func (archive *Archive) Location() string {
	return archive.path
}

func (archive *Archive) Contains(path string) bool {
	return util.FileExists(archive.encryptedFilePath(path))
}

func (archive *Archive) StoredFiles() ([]string, error) {
	basepath := archive.encryptedFilesRoot()
	if !util.FileExists(basepath) {
		return nil, nil
	}

	var storedFiles []string
	storedFilesChan := make(chan string, 128)
	filesToCollect := &sync.WaitGroup{}
	go func() {
		for stat := range storedFilesChan {
			storedFiles = append(storedFiles, stat)
			filesToCollect.Done()
		}
	}()

	err := cwalk.Walk(afero.Afero{Fs: afero.NewOsFs()}, basepath, func(path string, d fs.FileInfo, err error) error {
		if err != nil {
			fmt.Printf("ERROR: file: %v", err)
			return err
		}

		if !d.IsDir() && strings.HasSuffix(path, encryptedFileExtension) {
			storedFilePath := path
			storedFilePath = strings.TrimSuffix(storedFilePath, encryptedFileExtension)
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

func (archive *Archive) FileInfo(filename string) (*archive2.FileInfo, error) {
	fileInfo, stream, err := archive.OpenFile(filename)
	if err != nil {
		return nil, err
	}
	defer stream.Close()

	return &fileInfo, nil
}

func (archive *Archive) ListFiles() ([]*archive2.FileInfo, error) {
	return util.CollectFiles(archive)
}

func (archive *Archive) OpenFile(filename string) (archive2.FileInfo, io.ReadCloser, error) {
	return archive.OpenSeekableFile(filename)
}

func (archive *Archive) OpenSeekableFile(filename string) (archive2.FileInfo, io.ReadSeekCloser, error) {
	metadata, dataStream, err := archive.openDecryptionStream(filename)
	if err != nil {
		return archive2.FileInfo{}, nil, fmt.Errorf("open encrypted: %w", err)
	}

	return archive2.FileInfo{
		Path:   filename,
		Length: metadata.PrivateMetadata.Data.Length,
		Digest: metadata.PrivateMetadata.Data.Digest,
	}, dataStream, nil
}

func (archive *Archive) OpenReaderAtFile(filename string) (archive2.FileInfo, archive2.FileReadAtCloser, error) {
	metadata, dataStream, err := archive.openDecryptionStream(filename)
	if err != nil {
		return archive2.FileInfo{}, nil, fmt.Errorf("open encrypted: %w", err)
	}

	readerAt := ioutil.NewReaderAtFromSeeker(dataStream)

	return archive2.FileInfo{
		Path:   filename,
		Length: metadata.PrivateMetadata.Data.Length,
		Digest: metadata.PrivateMetadata.Data.Digest,
	}, readerAt, nil
}

func (archive *Archive) FileChecksum(filename string) (string, error) {
	fileInfo, s, err := archive.openDecryptionStream(filename)
	if err != nil {
		return "", fmt.Errorf("open encrypted: %w", err)
	}
	defer s.Close()

	return fileInfo.PrivateMetadata.Data.Digest["SHA256"], nil
}

func (archive *Archive) readEncrypted(filename string) (*akcf.FileInfo, []byte, error) {
	file, err := os.Open(archive.encryptedFilePath(filename))
	if err != nil {
		return nil, nil, fmt.Errorf("open: %w", err)
	}
	defer file.Close()

	return akcf.Decrypt(file, &akcf.DecryptOptions{
		Keyring:    archive.keyring,
		SigKeyring: archive.keyring,
	})
}

func (archive *Archive) openDecryptionStream(filename string) (*akcf.FileInfo, io.ReadSeekCloser, error) {
	file, err := os.Open(archive.encryptedFilePath(filename))
	if err != nil {
		return nil, nil, fmt.Errorf("open: %w", err)
	}
	defer func() {
		if file != nil {
			file.Close()
		}
	}()

	info, decryptStream, err := akcf.DecryptSeekableStream(file, &akcf.DecryptOptions{
		Keyring:    archive.keyring,
		SigKeyring: archive.keyring,
	})

	rsc := &readSeekerWithSeparateCloser{
		ReadSeeker: decryptStream,
		Closer:     file,
	}
	file = nil

	return info, rsc, err
}

func (archive *Archive) SaveFileFromBytes(content []byte, filename string) error {
	return archive.SaveFile(
		bytes.NewReader(content),
		&archive2.FileInfo{
			Path:   filename,
			Length: int64(len(content)),
			Digest: map[string]string{"SHA256": util.ComputeChecksumFromBytes(content)},
		},
	)
}

func (archive *Archive) SaveFile(reader io.Reader, fileInfo *archive2.FileInfo) error {
	checksum := fileInfo.Digest["SHA256"]

	destinationPath := archive.encryptedFilePath(fileInfo.Path)
	err := os.MkdirAll(paths.Dir(destinationPath), directoryCreationPerm)
	if err != nil {
		return fmt.Errorf("make directory for file: %w", err)
	}

	targetFile, err := os.OpenFile(destinationPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, fileCreationPerm)
	if err != nil {
		return err
	}

	processFailureError := func(s string, err error) error {
		// TODO: handle interruption signal (currently, this is not being called)
		fmt.Fprintf(os.Stderr, "save failed, removing: %s\n", destinationPath)

		removeErr := os.Remove(destinationPath)
		if removeErr != nil {
			return fmt.Errorf("%s, remove of corrupted file failed: %w", s, removeErr)
		}

		return fmt.Errorf("save file: %s: %w", s, err)
	}

	err = akcf.Encrypt(targetFile, reader, fileInfo.Length, map[string]string{
		"SHA256": checksum,
	}, &akcf.EncryptOptions{
		BoxKey:     archive.keyring.BoxKey,
		SigningKey: archive.keyring.SigningKey,
	})
	if err != nil {
		return processFailureError("store file", err)
	}

	_, contents, err := archive.readEncrypted(fileInfo.Path)
	if err != nil {
		return processFailureError("reopen stored file", err)
	}

	storedChecksum := util.ComputeChecksumFromBytes(contents)
	if storedChecksum != fileInfo.Digest["SHA256"] {
		err = fmt.Errorf("copied file has wrong checksum: got=%s, expected=%s", storedChecksum, checksum)
		return processFailureError("copy result", err)
	}

	return nil
}

func (archive *Archive) MoveFile(from string, to string) error {
	err := os.MkdirAll(paths.Dir(archive.encryptedFilePath(to)), directoryCreationPerm)
	if err != nil {
		return fmt.Errorf("create dirs for file: %w", err)
	}

	if archive.Contains(to) {
		return fmt.Errorf("destination file already exists: %s", to)
	}

	return os.Rename(
		archive.encryptedFilePath(from),
		archive.encryptedFilePath(to),
	)
}

func (archive *Archive) DeleteFile(filename string) error {
	return os.Remove(archive.encryptedFilePath(filename))
}

func (archive *Archive) VerifyFileIntegrity(path string) error {
	metadata, contents, err := archive.readEncrypted(path)
	if err != nil {
		return fmt.Errorf("open encrypted file: %w", err)
	}

	sum := util.ComputeChecksumFromBytes(contents)

	if sum != metadata.PrivateMetadata.Data.Digest["SHA256"] {
		return fmt.Errorf("file was modified")
	}

	return nil
}

func (archive *Archive) ChangeKeyringPassword(passwordProvider func() (string, error)) error {
	return archive.saveKeyring(passwordProvider)
}

func (archive *Archive) encryptedFilesRoot() string {
	return paths.Join(archive.path, "EncryptedFiles")
}

func (archive *Archive) encryptedFilePath(filename string) string {
	return paths.Join(archive.encryptedFilesRoot(), filename+encryptedFileExtension)
}

func (archive *Archive) create(
	passwordProvider func() (string, error),
) error {
	// TODO: verify directory doesn't exist or is empty

	basicKeyring := basic.NewKeyring()

	boxKey, err := basicKeyring.GenerateBoxKey()
	if err != nil {
		return fmt.Errorf("generate box key: %w", err)
	}

	signingKey, err := basicKeyring.GenerateSigningKey()
	if err != nil {
		return fmt.Errorf("generate signing key: %w", err)
	}

	archive.keyring = &keyring{
		BoxKey:     *boxKey,
		SigningKey: *signingKey,
	}

	return archive.saveKeyring(passwordProvider)
}

func (archive *Archive) saveKeyring(passwordProvider func() (string, error)) error {
	password, err := passwordProvider()
	if err != nil {
		return fmt.Errorf("get password: %w", err)
	}

	return josesecrets.SaveToFile(archive.keyringPath(), archive.keyring, password)
}

func (archive *Archive) open(
	passwordProvider func() (string, error),
) error {
	f, err := os.Open(archive.keyringPath())
	if err != nil {
		return fmt.Errorf("open keyring for load: %w", err)
	}
	defer f.Close()

	password, err := passwordProvider()
	if err != nil {
		return fmt.Errorf("get password: %w", err)
	}

	k := &keyring{}
	err = josesecrets.ReadUnmarshal(&k, f, password)
	if err != nil {
		return err
	}

	archive.keyring = k
	return nil
}

func (archive *Archive) keyringPath() string {
	return paths.Join(archive.path, "ArchiveKeep", "encryption", "keyring")
}
