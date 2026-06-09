package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepositoryAssociationGroupId
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.driver.fixtures.FixtureRepo
import org.archivekeep.files.driver.fixtures.FixtureRepoBuilder
import org.archivekeep.files.driver.inmemory.InMemoryLocalRepo
import org.archivekeep.files.driver.inmemory.InMemoryRepo

data class DemoRepository(
    val displayName: String,
    val id: String = "a-${displayName.toSlug()}",
    val correlationId: RepositoryAssociationGroupId? = "a-${displayName.toSlug()}",
    val physicalLocation: String = "unknown",
    val contentsFixture: FixtureRepo = FixtureRepo(emptyMap()),
    val repoFactory: (fixture: FixtureRepo, metadata: RepositoryMetadata) -> Repo = { fixture, metadata ->
        InMemoryRepo(
            fixture.contents.mapValues { (_, v) -> v.toByteArray() },
            metadata = metadata,
        )
    },
) {
    fun uriInStorage(storage: StorageNamedReference) = DemoRepositoryURIData(storage.uri.data, id).toURI()

    fun localInMemoryFactory(): DemoRepository =
        this.copy(
            repoFactory = { fixture, metadata ->
                InMemoryLocalRepo(
                    initialContents = fixture.contents.mapValues { (_, v) -> v.toByteArray() },
                    initialUnindexedContents = fixture.uncommittedContents.mapValues { (_, v) -> v.toByteArray() },
                    initialMissingContents = fixture.missingContents.mapValues { (_, v) -> v.toByteArray() },
                    metadata = metadata,
                )
            },
        )

    fun withContents(modifications: FixtureRepoBuilder.() -> Unit): DemoRepository =
        this.copy(
            contentsFixture = contentsFixture.derive(modifications),
        )

    fun withNewContents(modifications: FixtureRepoBuilder.() -> Unit): DemoRepository =
        this.copy(
            contentsFixture = FixtureRepoBuilder().also(modifications).build(),
        )
}
