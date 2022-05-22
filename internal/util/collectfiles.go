package util

import (
	"fmt"
	"sort"
	"sync"

	"github.com/archivekeep/archivekeep/archive"
)

const WorkerCount = 128

type FileCollectibleArchive interface {
	StoredFiles() ([]string, error)
	FileInfo(filename string) (*archive.FileInfo, error)
}

func CollectFiles(a FileCollectibleArchive) ([]*archive.FileInfo, error) {
	filenames, err := a.StoredFiles()
	if err != nil {
		return nil, err
	}

	return statFiles(a, filenames)
}

func statFiles(a FileCollectibleArchive, allFiles []string) ([]*archive.FileInfo, error) {

	fileToStatChan := make(chan string, 120)
	fileStatToCollectChan := make(chan *archive.FileInfo, 120)

	// TODO: improve error collection
	var anyError error

	files := make([]*archive.FileInfo, 0, len(allFiles))
	filesToCollect := sync.WaitGroup{}
	go func() {
		for stat := range fileStatToCollectChan {
			files = append(files, stat)
			filesToCollect.Done()
		}
	}()

	fileToStat := sync.WaitGroup{}
	for i := 0; i < WorkerCount; i++ {
		go func() {
			for filename := range fileToStatChan {
				// TODO: err
				info, err := a.FileInfo(filename)
				if err != nil {
					anyError = fmt.Errorf("get checksum of %s: %w", filename, err)
				}

				filesToCollect.Add(1)
				fileStatToCollectChan <- info

				fileToStat.Done()
			}
		}()
	}

	fileToStat.Add(len(allFiles))
	for _, filename := range allFiles {
		fileToStatChan <- filename
	}
	fileToStat.Wait()
	filesToCollect.Wait()
	close(fileToStatChan)
	close(fileStatToCollectChan)

	if anyError != nil {
		return nil, anyError
	}

	sort.Slice(files, func(i, j int) bool {
		return files[i].Path < files[j].Path
	})

	return files, nil
}
