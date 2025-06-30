package org.archivekeep.files.repo.encryptedfiles

import org.archivekeep.files.repo.RepoContractTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory

class EncryptedFileSystemRepositoryContractTest : RepoContractTest<EncryptedFileSystemRepository>() {
    @field:TempDir
    lateinit var tempPath: Path

    override suspend fun createNew(): TestRepo<EncryptedFileSystemRepository> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        return EncryptedFileSystemRepositoryTestRepo.createIn(path)
    }
}
