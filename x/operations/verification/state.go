package verification

import (
	"encoding/json"
	"sync"
	"time"
)

type successResult struct {
	Checksum []map[string]string

	VerifiedAt time.Time
}

type errorResult struct {
	Checksum []map[string]string

	VerifiedAt time.Time

	VerificationError string
}

type State struct {
	correctFiles map[string]successResult
	errorFiles   map[string]errorResult

	m sync.Mutex
}

func (s *State) addSuccess(path string) {
	s.m.Lock()
	defer s.m.Unlock()

	s.correctFiles[path] = successResult{
		Checksum:   nil,
		VerifiedAt: time.Now(),
	}
}

func (s *State) addCorrupted(path string, err error) {
	s.m.Lock()
	defer s.m.Unlock()

	s.errorFiles[path] = errorResult{
		Checksum:          nil,
		VerifiedAt:        time.Now(),
		VerificationError: err.Error(),
	}
}

func (s *State) numberOfCorruptedFiles() int {
	s.m.Lock()
	defer s.m.Unlock()

	return len(s.errorFiles)
}

type stateJSON struct {
	CorrectFiles map[string]successResult
	ErrorFiles   map[string]errorResult
}

func (s *State) UnmarshalJSON(bytes []byte) error {
	s.m.Lock()
	defer s.m.Unlock()

	var j stateJSON

	err := json.Unmarshal(bytes, &j)
	if err != nil {
		return err
	}

	s.correctFiles = j.CorrectFiles
	s.errorFiles = j.ErrorFiles

	return nil
}

func (s *State) MarshalJSON() ([]byte, error) {
	s.m.Lock()
	defer s.m.Unlock()

	return json.Marshal(stateJSON{
		CorrectFiles: s.correctFiles,
		ErrorFiles:   s.errorFiles,
	})
}
