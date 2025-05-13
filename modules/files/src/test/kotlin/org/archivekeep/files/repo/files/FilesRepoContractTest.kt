package org.archivekeep.files.repo.files

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FilesRepoContractTest : RepoContractTest<FilesRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override fun createNew(testDispatcher: TestDispatcher): FilesRepo {
        return FilesRepo(tempPath, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
    }
}
