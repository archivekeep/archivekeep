package org.archivekeep.testing.fixtures

class FixtureRepoBuilder(
    base: Map<String, String> = emptyMap(),
    baseUncommitted: Map<String, String> = emptyMap(),
) {
    val repoContents = base.toMutableMap()
    val repoUncommittedContents = baseUncommitted.toMutableMap()
    val repoMissingContents = baseUncommitted.toMutableMap()

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

    fun addMissing(
        path: String,
        contents: String = path,
    ) {
        repoMissingContents[path] = contents
    }

    fun deletePattern(regex: Regex) {
        repoContents.keys.removeAll(regex::matches)
    }

    fun moveToUncommitted(regex: Regex) {
        val keysToMove = repoContents.keys.filter(regex::matches).toSet()

        repoUncommittedContents.putAll(repoContents.filter { it.key in keysToMove })
        repoContents.keys.removeAll(keysToMove)
    }

    fun addFrom(fixtureRepo: FixtureRepo) {
        repoContents.putAll(fixtureRepo.contents)
        repoUncommittedContents.putAll(fixtureRepo.uncommittedContents)
        repoMissingContents.putAll(fixtureRepo.missingContents)
    }

    fun build() = FixtureRepo(this)
}
