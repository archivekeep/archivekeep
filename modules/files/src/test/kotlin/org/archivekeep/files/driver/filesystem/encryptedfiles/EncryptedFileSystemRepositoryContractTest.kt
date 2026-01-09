package org.archivekeep.files.driver.filesystem.encryptedfiles

import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
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

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
