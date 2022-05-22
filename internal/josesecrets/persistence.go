package josesecrets

import (
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"os"

	"gopkg.in/square/go-jose.v2"
)

func ReadUnmarshalFile(target interface{}, fileName, password string) error {
	f, err := os.Open(fileName)
	if err != nil {
		return err
	}
	defer f.Close()

	s, err := ioutil.ReadAll(f)
	if err != nil {
		return fmt.Errorf("read: %w", err)
	}

	object, err := jose.ParseEncrypted(string(s))
	if err != nil {
		return fmt.Errorf("parse encrypted keyring: %w", err)
	}

	decrypted, err := object.Decrypt(password)
	if err != nil {
		return fmt.Errorf("decrypt: %w", err)
	}

	return json.Unmarshal(decrypted, target)
}

func ReadUnmarshal(target interface{}, r io.Reader, password string) error {
	s, err := ioutil.ReadAll(r)
	if err != nil {
		return fmt.Errorf("read: %w", err)
	}

	object, err := jose.ParseEncrypted(string(s))
	if err != nil {
		return fmt.Errorf("parse encrypted keyring: %w", err)
	}

	decrypted, err := object.Decrypt(password)
	if err != nil {
		return fmt.Errorf("decrypt: %w", err)
	}

	return json.Unmarshal(decrypted, target)
}

func MarshalWrite(w io.Writer, data interface{}, password string) error {
	encrypter, err := jose.NewEncrypter(jose.A128GCM, jose.Recipient{
		Algorithm: jose.PBES2_HS256_A128KW,
		Key:       password,
	}, nil)
	if err != nil {
		return fmt.Errorf("create encrypter: %w", err)
	}

	keyringJson, err := json.MarshalIndent(data, "", "  ")
	if err != nil {
		return fmt.Errorf("marshal keyring json: %w", err)
	}

	object, err := encrypter.Encrypt(keyringJson)
	if err != nil {
		return fmt.Errorf("encrypt: %w", err)
	}

	_, err = w.Write([]byte(object.FullSerialize()))
	if err != nil {
		return fmt.Errorf("write: %w", err)
	}

	return nil
}
