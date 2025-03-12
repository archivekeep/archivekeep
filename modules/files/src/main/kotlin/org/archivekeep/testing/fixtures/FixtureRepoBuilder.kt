package org.archivekeep.testing.fixtures

class FixtureRepoBuilder(
    base: Map<String, String> = emptyMap(),
    baseUncommitted: Map<String, String> = emptyMap(),
) {
    val repoContents = base.toMutableMap()
    val repoUncommittedContents = baseUncommitted.toMutableMap()

    fun addStored(
        path: String,
        contents: String = path,
    ) {
        repoContents[path] = contents
    }

    fun addUncommitted(
        path: String,
        contents: String = path,
    ) {
        repoUncommittedContents[path] = contents
    }

    fun deletePattern(regex: Regex) {
        repoContents.keys.removeAll(regex::matches)
    }

    fun addFrom(fixtureRepo: FixtureRepo) {
        repoContents.putAll(fixtureRepo.contents)
        repoUncommittedContents.putAll(fixtureRepo.uncommittedContents)
    }

    fun build() = FixtureRepo(repoContents, repoUncommittedContents)
}
