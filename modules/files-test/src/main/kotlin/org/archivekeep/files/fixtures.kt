package org.archivekeep.files

import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.driver.fixtures.FixtureRepo

val testContents01 =
    FixtureRepo {
        addStored("A/01.txt")
        addStored("A/02.txt")
        addStored("B/03.txt")
    }

suspend fun <R : Repo> (R).withContentsFrom(fixtureRepo: FixtureRepo): R =
    this.also {
        fixtureRepo
            .contents
            .forEach {
                quickSave(it.key, it.value)
            }
    }
