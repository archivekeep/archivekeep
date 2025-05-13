package org.archivekeep.testing.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.repo.RepositoryMetadata

class InMemoryRepoContractTest : RepoContractTest<InMemoryRepo>() {
    override fun createNew(): TestRepo<InMemoryRepo> {
        return object : TestRepo<InMemoryRepo> {
            val metadataMutableStateFlow = MutableStateFlow(RepositoryMetadata())
            val contentsMutableStateFlow = MutableStateFlow<Map<String, ByteArray>>(emptyMap())

            override fun open(testDispatcher: TestDispatcher): InMemoryRepo {
                return InMemoryRepo(testDispatcher, metadataMutableStateFlow, contentsMutableStateFlow)
            }
        }
    }
}
