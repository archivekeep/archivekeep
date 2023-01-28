package cmd_test

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	paths "path"
	"strings"
	"sync"
	"testing"

	"golang.org/x/sync/errgroup"
	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/archive/remote"
	grpcarchive "github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	"github.com/archivekeep/archivekeep/internal/cli/cmd"
	"github.com/archivekeep/archivekeep/internal/cli/currentarchive"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
	"github.com/archivekeep/archivekeep/x/operations/sharing"
)

const password = "123-456"

func TestDefaultServe(t *testing.T) {
	m, archiveDir := createTestArchive(t)

	newClientCertificate, err := m.RegisterNewClient("test-client")
	assert.NilError(t, err)

	connectToken, err := remote.NewConnectToken(
		"127.0.0.1:24202",
		m.GetRootCertificate(),
		newClientCertificate,
	)
	assert.NilError(t, err)

	connection, err := connectToken.ToConnection()
	assert.NilError(t, err)

	interrupt := runInBackground(t, archiveDir, "sharing", "serve", "--api-grpc")
	defer func() {
		err := interrupt()
		assert.NilError(t, err)
	}()

	grpArchive, err := grpcarchive.Connect(connection, "archives/"+paths.Base(archiveDir))
	assert.NilError(t, err)

	{
		_, err = grpArchive.ListFiles()
		assert.NilError(t, err)
	}

	{
		err := grpArchive.SaveFile(strings.NewReader("some data"), &archive.FileInfo{
			Path:   "new file",
			Length: 9,
			Digest: map[string]string{
				"SHA256": "1307990e6ba5ca145eb35e99182a9bec46531bc54ddf656a602c780fa0240dee",
			},
		})
		assert.ErrorContains(t, err, "write access is denied")
	}

	{
		err := grpArchive.MoveFile("a", "new/a")
		assert.ErrorContains(t, err, "write access is denied")
	}
}

func TestWriteableServe(t *testing.T) {
	m, archiveDir := createTestArchive(t)

	newClientCertificate, err := m.RegisterNewClient("test-client")
	assert.NilError(t, err)

	connectToken, err := remote.NewConnectToken(
		"127.0.0.1:24202",
		m.GetRootCertificate(),
		newClientCertificate,
	)
	assert.NilError(t, err)

	connection, err := connectToken.ToConnection()
	assert.NilError(t, err)

	interrupt := runInBackground(t, archiveDir, "sharing", "serve", "--api-grpc", "--allow-write")
	defer func() {
		err := interrupt()
		assert.NilError(t, err)
	}()

	grpArchive, err := grpcarchive.Connect(connection, "archives/"+paths.Base(archiveDir))
	assert.NilError(t, err)

	{
		_, err = grpArchive.ListFiles()
		assert.NilError(t, err)
	}

	{
		err := grpArchive.SaveFile(strings.NewReader("some data"), &archive.FileInfo{
			Path:   "new file",
			Length: 9,
			Digest: map[string]string{
				"SHA256": "1307990e6ba5ca145eb35e99182a9bec46531bc54ddf656a602c780fa0240dee",
			},
		})
		assert.NilError(t, err)
		// TODO: check contents - read it back
	}

	{
		err := grpArchive.MoveFile("a", "new/a")
		assert.NilError(t, err)
		// TODO: check move - ensure old is missing, and new one is present
	}
}

func createTestArchive(t *testing.T) (*sharing.SingleArchive, string) {
	loc := testarchive.CreateTestingArchive01(t).Dir

	ca, err := currentarchive.OpenLocation(loc)
	assert.NilError(t, err)

	err = ca.OpenKeyring(func() (string, error) {
		return password, nil
	})
	assert.NilError(t, err)

	archiveSharingManager := sharing.NewSingleArchive(ca, ca.Keyring())

	err = archiveSharingManager.Init()
	assert.NilError(t, err)

	return archiveSharingManager, loc
}

func runInBackground(t *testing.T, archiveDir string, args ...string) func() error {
	t.Helper()

	ctx, cancel := context.WithCancel(context.Background())
	g, gCtx := errgroup.WithContext(ctx)

	outReader, outWriter := io.Pipe()

	outTeeReader := io.TeeReader(outReader, &TestLogWriter{t})
	bufOutReader := bufio.NewReader(outTeeReader)

	g.Go(func() error {
		defer outWriter.Close()

		return runCmdBase(gCtx, archiveDir, strings.NewReader(password+"\n"), outWriter, args...)
	})

	var waitStart sync.WaitGroup
	waitStart.Add(1)

	g.Go(func() error {
		defer waitStart.Done()

		// TODO: randomize port it's being listened on
		line, _, err := bufOutReader.ReadLine()
		assert.NilError(t, err)
		assert.Assert(t, strings.Contains(string(line), "Listening on"))

		go func() {
			ioutil.ReadAll(bufOutReader)
		}()

		return nil
	})

	waitStart.Wait()

	return func() error {
		cancel()

		return g.Wait()
	}
}

type TestLogWriter struct {
	*testing.T
}

func (t *TestLogWriter) Write(p []byte) (n int, err error) {
	t.Log(string(p))
	return len(p), nil
}

func runCmdBase(ctx context.Context, dir string, stdin io.Reader, stdout io.Writer, args ...string) error {
	currentDir, _ := os.Getwd()
	defer os.Chdir(currentDir)

	err := os.Chdir(dir)
	if err != nil {
		return fmt.Errorf("change CWD for command execution: %w", err)
	}

	c := cmd.New()
	c.SetIn(stdin)
	c.SetOut(stdout)
	c.SetArgs(args)
	err = c.ExecuteContext(ctx)

	return err
}
