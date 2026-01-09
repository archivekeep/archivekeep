package org.archivekeep.testing.storage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.files.driver.inmemory.InMemoryRepo
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.repo.RepositoryMetadata

class InMemoryRepoContractTest : RepoContractTest<InMemoryRepo>() {
    override suspend fun createNew(): TestRepo<InMemoryRepo> =
        object : TestRepo<InMemoryRepo> {
            val metadataMutableStateFlow = MutableStateFlow(RepositoryMetadata())
            val contentsMutableStateFlow = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
            val missingContentsMutableStateFlow = MutableStateFlow<Map<String, ByteArray>>(emptyMap())

            override suspend fun open(testDispatcher: CoroutineDispatcher): InMemoryRepo =
                InMemoryRepo(
                    testDispatcher,
                    metadataMutableStateFlow,
                    contentsMutableStateFlow,
                    missingContentsMutableStateFlow,
                )
        }
}
