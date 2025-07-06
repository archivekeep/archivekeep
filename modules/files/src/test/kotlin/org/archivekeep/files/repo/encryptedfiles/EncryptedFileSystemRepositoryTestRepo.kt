package org.archivekeep.files.repo.encryptedfiles

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest.TestRepo
import java.nio.file.Path
import java.util.UUID

class EncryptedFileSystemRepositoryTestRepo(
    val path: Path,
    val password: String,
) : TestRepo<EncryptedFileSystemRepository> {
    companion object {
        suspend fun createIn(path: Path): EncryptedFileSystemRepositoryTestRepo {
            val password = UUID.randomUUID().toString()

            EncryptedFileSystemRepository.create(path, password)

            return EncryptedFileSystemRepositoryTestRepo(path, password)
        }
    }

    override suspend fun open(testDispatcher: TestDispatcher): EncryptedFileSystemRepository =
        EncryptedFileSystemRepository.openAndUnlock(path, password, stateDispatcher = testDispatcher)
}
