package org.archivekeep.testing.storage

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest

class InMemoryRepoContractTest : RepoContractTest<InMemoryRepo>() {
    override fun createNew(testDispatcher: TestDispatcher): InMemoryRepo {
        return InMemoryRepo(stateDispatcher = testDispatcher)
    }
}
