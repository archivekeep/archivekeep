package org.archivekeep.files.procedures.addpush

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.driver.fixtures.FixtureRepoBuilder
import org.archivekeep.files.driver.inmemory.toInMemoryLocalRepo
import org.archivekeep.files.driver.inmemory.toInMemoryRepo
import org.archivekeep.files.fromStringContents
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.shouldHaveCommittedContentsOf
import kotlin.test.Test

class AddAndPushProcedureJobTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun execute() =
        runTest {
            val baseRepo =
                FixtureRepoBuilder()
                    .apply {
                        addStored("base.txt")
                        addUncommitted("A/01.txt")
                        addUncommitted("B/01.txt")
                        addUncommitted("C/01.txt")
                        addMissing("OLD/A/01.txt", "A/01.txt")
                    }.build()
                    .toInMemoryLocalRepo()

            val other1 =
                FixtureRepoBuilder()
                    .apply {
                        addStored("1/extra.txt")
                        addStored("OLD/A/01.txt", "A/01.txt")
                    }.build()
                    .toInMemoryRepo()

            val other2 =
                FixtureRepoBuilder()
                    .apply {
                        addStored("2/extra.txt")
                        addStored("OLD/A/01.txt", "A/01.txt")
                    }.build()
                    .toInMemoryRepo()

            val fta =
                listOf(
                    IndexUpdateProcedure.PreparationResult.NewFile.fromStringContents("B/01.txt"),
                )
            val mta =
                listOf(
                    IndexUpdateProcedure.PreparationResult.Move(
                        "TODO",
                        "A/01.txt".toByteArray().size.toLong(),
                        "OLD/A/01.txt",
                        "A/01.txt",
                    ),
                )

            val job =
                AddAndPushProcedureJob(
                    repositoryProvider = {
                        mapOf<String, Repo>(
                            "base" to baseRepo,
                            "other-1" to other1,
                            "other-2" to other2,
                        )[it]!!
                    },
                    "base",
                    IndexUpdateProcedure.PreparationResult(
                        newFiles = fta,
                        moves = mta,
                        emptyList(),
                        emptyMap(),
                    ),
                    fta.toSet(),
                    mta.toSet(),
                    setOf("other-1", "other-2"),
                )

            job.run()

            advanceUntilIdle()

            baseRepo shouldHaveCommittedContentsOf
                FixtureRepoBuilder()
                    .apply {
                        addStored("base.txt")
                        addStored("A/01.txt")
                        addStored("B/01.txt")
                    }.build()

            other1 shouldHaveCommittedContentsOf
                FixtureRepoBuilder()
                    .apply {
                        addStored("1/extra.txt")
                        addStored("A/01.txt")
                        addStored("B/01.txt")
                    }.build()

            other2 shouldHaveCommittedContentsOf
                FixtureRepoBuilder()
                    .apply {
                        addStored("2/extra.txt")
                        addStored("A/01.txt")
                        addStored("B/01.txt")
                    }.build()
        }
}
