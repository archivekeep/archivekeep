package grpcarchive_test

import (
	"io/ioutil"
	"net"
	"testing"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"gotest.tools/v3/assert"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/archive/remote"
	"github.com/archivekeep/archivekeep/archive/remote/client/grpc"
	"github.com/archivekeep/archivekeep/internal/tests/testarchive"
	"github.com/archivekeep/archivekeep/server/api"
	"github.com/archivekeep/archivekeep/server/api/grpc"
	"github.com/archivekeep/archivekeep/x/operations/sharing/servertls"
	"github.com/archivekeep/archivekeep/x/testing/archivetest"
)

func TestArchiveContract(t *testing.T) {
	archivetest.Run(t, createImplementationTester())
}

func createImplementationTester() *archivetest.ImplementationTester {
	return &archivetest.ImplementationTester{
		New: func(t *testing.T) *archivetest.TestedArchive {
			a, _ := testarchive.Create(t)

			serverCertificates, err := servertls.GenerateCertificates()
			assert.NilError(t, err)

			grpcServer := grpc_ak.NewServer(
				&api.TestSingleArchiveService{
					A: api.TestArchive{
						Id:          "my-archive",
						DisplayName: "My test archive",
						A:           a,
					},
				},
				nil,
				grpc.Creds(credentials.NewTLS(serverCertificates.ServerTLSConfig())),
			)

			lis, err := net.Listen("tcp", "localhost:0")
			if err != nil {
				t.Error(err)
			}

			go func() {
				err := grpcServer.Serve(lis)
				if err != nil {
					t.Error(err)
				}
			}()
			t.Cleanup(grpcServer.GracefulStop)

			newClientCertificate, err := serverCertificates.RegisterNewClient("test-client")
			assert.NilError(t, err)

			connectToken, err := remote.NewConnectToken(
				lis.Addr().String(),
				serverCertificates.GetRootCertificate(),
				newClientCertificate,
			)
			assert.NilError(t, err)

			connection, err := connectToken.ToConnection()
			assert.NilError(t, err)

			return &archivetest.TestedArchive{
				OpenReadWriter: func() archive.ReadWriter {
					conn, err := grpc.Dial(
						lis.Addr().String(),
						grpc.WithTransportCredentials(
							credentials.NewTLS(connection.ClientTLSConfig()),
						),
					)
					assert.NilError(t, err)
					t.Cleanup(func() {
						conn.Close()
					})

					return grpcarchive.NewRemoteArchive(conn, "my-archive")
				},
				StoreFile: func(path string, contents []byte) {
					err := a.SaveFileFromBytes(contents, path)
					assert.NilError(t, err)
				},
				ReadFileContents: func(path string) []byte {
					_, readCloser, err := a.OpenFile(path)
					assert.NilError(t, err)

					data, err := ioutil.ReadAll(readCloser)
					assert.NilError(t, err)

					return data
				},
			}
		},
	}
}
