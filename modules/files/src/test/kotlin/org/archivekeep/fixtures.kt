package org.archivekeep

import org.archivekeep.files.repo.Repo

suspend fun (Repo).populateTestContents01() {
    quickSave("A/01.txt")
    quickSave("A/02.txt")
    quickSave("B/03.txt")
}
