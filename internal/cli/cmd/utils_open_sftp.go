package cmd

import (
	"fmt"
	"net"
	"os"
	"regexp"
	"strings"

	"github.com/pkg/sftp"
	"github.com/spf13/afero"
	"golang.org/x/crypto/ssh"
	"golang.org/x/crypto/ssh/agent"

	"github.com/archivekeep/archivekeep/internal/afero/sftpfs"
)

var sshRegex = regexp.MustCompile("^(?P<user>.*?)@(?P<host>[^:/]+)(?::(?P<port>[0-9]*)?)?(?P<path>.*?)$")

func resolveSSH(location string) (afero.Fs, string, error) {
	locationParts := sshRegex.FindStringSubmatch(strings.TrimPrefix(location, "ssh://"))
	if len(locationParts) == 0 {
		return nil, "", fmt.Errorf("invalid url")
	}
	port := locationParts[3]
	if port == "" {
		port = "22"
	}

	socket := os.Getenv("SSH_AUTH_SOCK")
	conn, err := net.Dial("unix", socket)
	if err != nil {
		return nil, "", fmt.Errorf("failed to open SSH_AUTH_SOCK: %v", err)
	}

	agentClient := agent.NewClient(conn)
	config := &ssh.ClientConfig{
		User: locationParts[1],
		Auth: []ssh.AuthMethod{
			ssh.PublicKeysCallback(agentClient.Signers),
		},
		// TODO
		HostKeyCallback: ssh.InsecureIgnoreHostKey(),
	}

	sshc, err := ssh.Dial("tcp", locationParts[2]+":"+port, config)
	if err != nil {
		return nil, "", err
	}

	sftpc, err := sftp.NewClient(sshc)
	if err != nil {
		return nil, "", err
	}

	f := sftpfs.New(sftpc)

	return f, locationParts[4], nil
}
