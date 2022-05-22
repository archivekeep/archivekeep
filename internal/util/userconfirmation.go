package util

import (
	"bufio"
	"bytes"
	"fmt"
	"os"
	"strings"
	"syscall"

	"golang.org/x/term"

	"github.com/spf13/cobra"
)

func AskForConfirmation(cmd *cobra.Command, s string, args ...interface{}) bool {
	stdin := bufio.NewReader(cmd.InOrStdin())

	for {
		cmd.Printf(s+" [y/n]: ", args...)

		answer, err := stdin.ReadString('\n')
		if err != nil {
			panic(err)
		}

		answer = strings.ToLower(strings.TrimSpace(answer))

		if answer == "y" || answer == "yes" {
			return true
		} else if answer == "n" || answer == "no" {
			return false
		}
	}
}

func AskForInput(cmd *cobra.Command, s string) string {
	var (
		answer        = ""
		resultErrChan = make(chan error)
	)

	stdin := bufio.NewReader(cmd.InOrStdin())

	go func() {
		cmd.Printf("%s: ", s)

		answerA, err := stdin.ReadString('\n')
		if err != nil {
			resultErrChan <- err
		}

		answer = strings.TrimRight(answerA, "\n")
		resultErrChan <- nil
	}()

	select {
	case resultErr := <-resultErrChan:
		if resultErr != nil {
			panic(resultErr)
		}
		return answer
	case <-cmd.Context().Done():
		os.Stdout.Write([]byte("\n"))
		panic(fmt.Errorf("interrupted"))
	}
}

func PasswordPrompt(cmd *cobra.Command, s string) func() (string, error) {
	return func() (string, error) {
		return AskForPassword(cmd, s)
	}
}

func PasswordDoublePrompt(cmd *cobra.Command, s string) func() (string, error) {
	return func() (string, error) {
		for {
			password1, err := AskForPassword(cmd, s)
			if err != nil {
				return "", err
			}

			password2, err := AskForPassword(cmd, "Verify - "+s)
			if err != nil {
				return "", err
			}

			if password1 != password2 {
				continue
			}

			return password1, nil
		}
	}
}

func AskForPassword(cmd *cobra.Command, s string) (string, error) {
	var (
		password      = ""
		resultErrChan = make(chan error)
	)

	in := cmd.InOrStdin()

	if in == os.Stdin {
		oldState, err := term.GetState(syscall.Stdin)
		if err != nil {
			return "", err
		}
		defer func() {
			term.Restore(syscall.Stdin, oldState)
		}()

		go func() {
			os.Stdout.WriteString(s)
			passwordBytes, err := term.ReadPassword(syscall.Stdin)
			if err != nil {
				resultErrChan <- err
				return
			}
			os.Stdout.Write([]byte("\n"))
			password = strings.TrimSpace(string(passwordBytes))
			resultErrChan <- nil
		}()

		select {
		case resultErr := <-resultErrChan:
			return password, resultErr
		case <-cmd.Context().Done():
			os.Stdout.Write([]byte("\n"))
			return "", fmt.Errorf("interrupted")
		}
	} else {
		var buf bytes.Buffer
		var readBuf [1]byte

		for {
			_, err := in.Read(readBuf[:])
			if err != nil {
				return "", err
			}

			if readBuf[0] == '\n' {
				return buf.String(), nil
			}

			buf.Write(readBuf[:])
		}
	}
}
