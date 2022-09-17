package sharing

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"net/http"

	"golang.org/x/sync/errgroup"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/archivekeep/archivekeep/archive"
	"github.com/archivekeep/archivekeep/internal/josesecrets"
	grpc_ak "github.com/archivekeep/archivekeep/server/api/grpc"
	"github.com/archivekeep/archivekeep/server/api/rest"
	"github.com/archivekeep/archivekeep/x/operations/sharing/servertls"
)

const sharingSecretsKey = "sharing"

type SingleArchive struct {
	arw          archive.ReadWriter
	secretsStore *josesecrets.Keyring

	certificates *servertls.Certificates
}

func NewSingleArchive(
	arw archive.ReadWriter,
	secretsStore *josesecrets.Keyring,
) *SingleArchive {
	return &SingleArchive{
		arw:          arw,
		secretsStore: secretsStore,
	}
}

func (m *SingleArchive) Init() error {
	if m.secretsStore.Contains(sharingSecretsKey) {
		var certificates servertls.Certificates

		err := m.secretsStore.GetTo(sharingSecretsKey, &certificates)
		if err != nil {
			return fmt.Errorf("get certificates from keyring: %w", err)
		}

		// TODO: check certificates are valid
		m.certificates = &certificates

		return nil
	}

	var certificates *servertls.Certificates
	err := m.secretsStore.Update(func(values map[string]json.RawMessage) error {
		var err error

		if _, exists := values[sharingSecretsKey]; exists {
			return fmt.Errorf("created in mean time")
		}

		certificates, err = servertls.GenerateCertificates()
		if err != nil {
			return fmt.Errorf("generate certificates: %w", err)
		}

		newValue, err := json.Marshal(certificates)
		if err != nil {
			return fmt.Errorf("serialize certificates")
		}

		values[sharingSecretsKey] = newValue

		return nil
	})
	if err != nil {
		return err
	}

	m.certificates = certificates

	return nil
}

func (m *SingleArchive) GetRootCertificate() *x509.Certificate {
	return m.certificates.GetRootCertificate()
}

func (m *SingleArchive) RegisterNewClient(name string) (*tls.Certificate, error) {
	if m.certificates == nil {
		// TODO: should be only re-open
		err := m.Init()
		if err != nil {
			return nil, fmt.Errorf("init config: %w", err)
		}
	}

	var newClientCertificate *tls.Certificate
	var newCertificates servertls.Certificates

	err := m.secretsStore.Update(func(values map[string]json.RawMessage) error {
		err := json.Unmarshal(values[sharingSecretsKey], &newCertificates)
		if err != nil {
			return fmt.Errorf("read config: %w", err)
		}

		newClientCertificate, err = newCertificates.RegisterNewClient(name)
		if err != nil {
			return fmt.Errorf("register client: %w", err)
		}

		newValue, err := json.Marshal(&newCertificates)
		if err != nil {
			return fmt.Errorf("serialize certificates")
		}

		values[sharingSecretsKey] = newValue

		return nil
	})
	if err != nil {
		return nil, err
	}

	// TODO: don't do this
	*m.certificates = newCertificates

	return newClientCertificate, nil
}

func (m *SingleArchive) GenerateNewCredentialsForClient(name string) error {
	return fmt.Errorf("not implemented")
}

func (m *SingleArchive) RemoveClient(name string) error {
	return fmt.Errorf("not implemented")
}

func (m *SingleArchive) Serve(
	currentArchiveName string,
	apiGrpc bool,
	apiInsecureREST bool,
	grpcHost string,
	grpcPort string,
) (*Server, error) {
	g, _ := errgroup.WithContext(context.Background())
	server := &Server{
		serveGroup: g,
	}

	singleArchiveService := &singleArchiveService{
		name:              currentArchiveName,
		archiveReadWriter: m.arw,
	}

	if apiGrpc {
		if m.certificates == nil {
			// TODO: should be only re-open
			err := m.Init()
			if err != nil {
				return nil, fmt.Errorf("init config: %w", err)
			}
		}

		lis, err := net.Listen("tcp", net.JoinHostPort(grpcHost, grpcPort))
		if err != nil {
			return nil, err
		}

		grpcServer := grpc_ak.NewServer(
			singleArchiveService,
			nil,
			grpc.Creds(credentials.NewTLS(m.certificates.ServerTLSConfig())),
		)

		g.Go(func() error {
			return grpcServer.Serve(lis)
		})

		server.listener = lis
		server.grpcServer = grpcServer
	}

	if apiInsecureREST {
		restApi := rest.NewAPI(rest.Options{
			ArchiveService: singleArchiveService,
		})

		httpServer := &http.Server{
			Addr:    ":3000",
			Handler: restApi.Handler(),
		}

		g.Go(func() error {
			err := httpServer.ListenAndServe()

			if !errors.Is(err, http.ErrServerClosed) {
				return err
			}

			return nil
		})

		server.httpServer = httpServer
	}

	return server, nil
}
