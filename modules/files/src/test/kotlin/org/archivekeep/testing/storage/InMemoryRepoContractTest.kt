package org.archivekeep.testing.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.repo.RepositoryMetadata

class InMemoryRepoContractTest : RepoContractTest<InMemoryRepo>() {
    override fun createNew(): TestRepo<InMemoryRepo> =
        object : TestRepo<InMemoryRepo> {
            val metadataMutableStateFlow = MutableStateFlow(RepositoryMetadata())
            val contentsMutableStateFlow = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
            val missingContentsMutableStateFlow = MutableStateFlow<Map<String, ByteArray>>(emptyMap())

            override suspend fun open(testDispatcher: TestDispatcher): InMemoryRepo =
                InMemoryRepo(testDispatcher, metadataMutableStateFlow, contentsMutableStateFlow, missingContentsMutableStateFlow)
        }
}
