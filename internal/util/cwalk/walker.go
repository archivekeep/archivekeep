package cwalk

import (
	"fmt"
	"io/fs"
	"path/filepath"
	"sync"

	"github.com/spf13/afero"
)

const WorkerCount = 120

type walker struct {
	fs afero.Afero
	f  filepath.WalkFunc

	dirQueue chan string
	dirWG    sync.WaitGroup

	errors    []error
	errorsMux sync.Mutex
}

func (w *walker) start(count int) {
	w.dirQueue = make(chan string, 120)

	for i := 0; i < count; i++ {
		go w.worker()
	}
}

func (w *walker) worker() {
	for currentBasePath := range w.dirQueue {
		w.processDir(currentBasePath)
	}
}

func (w *walker) processDir(currentBasePath string) {
	defer w.dirWG.Done()

	err := w.fs.Walk(currentBasePath, func(path string, info fs.FileInfo, err error) error {
		err = w.f(path, info, err)
		if err != nil {
			return err
		}

		if info.IsDir() && path != currentBasePath {
			w.add(path)
			return filepath.SkipDir
		}

		return nil
	})

	if err != nil {
		w.errorsMux.Lock()
		defer w.errorsMux.Unlock()

		w.errors = append(w.errors, err)
	}
}

func (w *walker) add(dir string) {
	w.dirWG.Add(1)
	w.dirQueue <- dir

}

func (w *walker) wait() {
	w.dirWG.Wait()
}

func (w *walker) close() {
	close(w.dirQueue)
}

func Walk(fs afero.Afero, dir string, f filepath.WalkFunc) error {
	w := &walker{
		fs: fs,
		f:  f,
	}
	w.start(WorkerCount)
	w.add(dir)
	w.wait()
	w.close()

	if len(w.errors) > 0 {
		return fmt.Errorf("one on more errors: %v", w.errors)
	} else {
		return nil
	}
}
