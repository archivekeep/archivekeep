package josesecrets

import (
	"encoding/json"

	"github.com/archivekeep/archivekeep/internal/util"
)

type Keyring struct {
	fileName string
	password string

	values map[string]json.RawMessage
}

func OpenKeyring(path string, password string) (*Keyring, error) {
	k := &Keyring{
		fileName: path,
		password: password,
	}

	if util.FileExists(k.fileName) {
		err := ReadUnmarshalFile(&k.values, k.fileName, k.password)
		if err != nil {
			return nil, err
		}
	} else {
		k.values = map[string]json.RawMessage{}
	}

	return k, nil
}

func (k *Keyring) Put(name string, value interface{}) error {
	// TODO: what if computed `value` is now outdated, remove method

	return k.Update(func(values map[string]json.RawMessage) error {
		var err error
		values[name], err = json.Marshal(value)
		return err
	})
}

func (k *Keyring) Update(
	updateFn func(values map[string]json.RawMessage) error,
) error {
	newValues, err := UpdateFile(k.fileName, updateFn, k.password)
	if err != nil {
		return err
	}

	k.values = newValues
	return nil
}

func (k *Keyring) Contains(valueName string) bool {
	_, present := k.values[valueName]
	return present
}

func (k *Keyring) GetTo(
	valueName string,
	target interface{},
) error {
	return json.Unmarshal(k.values[valueName], target)
}
