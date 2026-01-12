package org.archivekeep.files.api.repository.operations

import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.utils.text.pathDiff
import java.io.PrintWriter

class CompareOperation {
    suspend fun execute(
        base: Repo,
        other: Repo,
    ): Result {
        val baseIndex = base.index()
        val otherIndex = other.index()

        return calculate(baseIndex, otherIndex)
    }

    fun calculate(
        baseIndex: RepoIndex,
        otherIndex: RepoIndex,
    ): Result {
        val allChecksums = baseIndex.byChecksumSha256.keys union otherIndex.byChecksumSha256.keys

        val zippedInstances =
            allChecksums.map { checksum ->
                object {
                    val checksum = checksum
                    val baseInstances = baseIndex.byChecksumSha256[checksum]
                    val otherInstances = otherIndex.byChecksumSha256[checksum]
                    val fileSize: Long? = ((baseInstances ?: emptyList()) + (otherInstances ?: emptyList())).firstNotNullOfOrNull { it.size }

                    fun asRelocationOrNull(): Result.Relocation? =
                        if (baseInstances != null && otherInstances != null) {
                            Result.Relocation(
                                checksum = checksum,
                                fileSize = fileSize,
                                baseFilenames = baseInstances.map { it.path }.sorted(),
                                otherFilenames = otherInstances.map { it.path }.sorted(),
                            )
                        } else {
                            null
                        }

                    fun asBaseExtraOrNull(): Result.ExtraGroup? =
                        if (baseInstances != null && otherInstances == null) {
                            Result.ExtraGroup(this.checksum, fileSize, baseInstances.map { it.path }.sorted())
                        } else {
                            null
                        }

                    fun asOtherExtraOrNull(): Result.ExtraGroup? =
                        if (otherInstances != null && baseInstances == null) {
                            Result.ExtraGroup(this.checksum, fileSize, otherInstances.map { it.path }.sorted())
                        } else {
                            null
                        }
                }
            }

        val relocations =
            zippedInstances
                .mapNotNull { it.asRelocationOrNull() }
                .filter { it.extraBaseLocations.isNotEmpty() || it.extraOtherLocations.isNotEmpty() }
                .sortedBy { it.baseFilenames[0] }

        val unmatchedBaseExtras = zippedInstances.mapNotNull { it.asBaseExtraOrNull() }.sortedBy { it.filenames[0] }
        val unmatchedOtherExtras = zippedInstances.mapNotNull { it.asOtherExtraOrNull() }.sortedBy { it.filenames[0] }

        val newContentAfterMove =
            relocations
                .flatMap { relocation ->
                    relocation.otherFilenames.filter { otherFilename ->
                        val baseFile = baseIndex.byPath[otherFilename]

                        baseFile != null && baseFile.checksumSha256 != relocation.checksum
                    }
                }

        val newContentToOverwrite =
            unmatchedOtherExtras
                .flatMap { otherExtra ->
                    otherExtra.filenames.filter { otherFilename ->
                        val baseFile = baseIndex.byPath[otherFilename]

                        baseFile != null && baseFile.checksumSha256 != otherExtra.checksum
                    }
                }

        return Result(
            allBaseFiles = baseIndex.files.map { it.path },
            allOtherFiles = otherIndex.files.map { it.path },
            relocations = relocations,
            newContentAfterMove = newContentAfterMove,
            newContentToOverwrite = newContentToOverwrite,
            unmatchedBaseExtras = unmatchedBaseExtras,
            unmatchedOtherExtras = unmatchedOtherExtras,
        )
    }

    class Result(
        val allBaseFiles: List<String>,
        val allOtherFiles: List<String>,
        val relocations: List<Relocation>,
        // Paths to be having new content, after original file in other archive is moved to a new path
        val newContentAfterMove: List<String>,
        val newContentToOverwrite: List<String>,
        val unmatchedBaseExtras: List<ExtraGroup>,
        val unmatchedOtherExtras: List<ExtraGroup>,
    ) {
        val hasRelocations: Boolean
            get() = relocations.isNotEmpty()

        data class Relocation(
            val checksum: String,
            val fileSize: Long?,
            val baseFilenames: List<String>,
            val otherFilenames: List<String>,
        ) {
            val extraBaseLocations = (baseFilenames.toSet() subtract otherFilenames.toSet()).toList()
            val extraOtherLocations = (otherFilenames.toSet() subtract baseFilenames.toSet()).toList()

            val isIncreasingDuplicates: Boolean
                get() = extraBaseLocations.size > extraOtherLocations.size

            val isDecreasingDuplicates: Boolean
                get() = extraOtherLocations.size > extraBaseLocations.size

            companion object
        }

        data class ExtraGroup(
            val checksum: String,
            val fileSize: Long?,
            val filenames: List<String>,
        ) {
            companion object
        }

        fun printAll(
            out: PrintWriter,
            baseName: String,
            otherName: String,
        ) {
            printUnmatchedBaseExtras(out, baseName, otherName)
            printUnmatchedOtherExtras(out, baseName, otherName)
            printRelocations(out, baseName, otherName)

            // TODO: show list of same filenames with different contents and other comparison results

            printStats(out, baseName, otherName)
        }

        private fun printUnmatchedBaseExtras(
            out: PrintWriter,
            baseName: String,
            otherName: String,
        ) {
            if (unmatchedBaseExtras.isNotEmpty()) {
                out.print("\nExtra files in $baseName archive:\n")

                unmatchedBaseExtras.forEach { baseExtra ->
                    out.print("\t${filenamesPrint(baseExtra.filenames)}\n")
                }
            }
        }

        private fun printUnmatchedOtherExtras(
            out: PrintWriter,
            baseName: String,
            otherName: String,
        ) {
            if (unmatchedOtherExtras.isNotEmpty()) {
                out.print("\nExtra files in $otherName archive:\n")

                unmatchedOtherExtras.forEach { otherExtra ->
                    out.print("\t${filenamesPrint(otherExtra.filenames)}\n")
                }
            }
        }

        private fun printRelocations(
            out: PrintWriter,
            baseName: String,
            otherName: String,
        ) {
            fun colorFunc(str: String): String = "\u001b[31m${str}\u001b[0m"

            if (relocations.isNotEmpty()) {
                out.println("\nFiles to be moved in $otherName to match $baseName:")

                relocations.forEach { r ->
                    if (r.extraBaseLocations.size == 1 && r.extraOtherLocations.size == 1) {
                        val p = pathDiff(r.extraOtherLocations[0], r.extraBaseLocations[0], ::colorFunc)

                        out.println("\t$p")
                    } else {
                        out.println("\t${filenamesPrint(r.extraOtherLocations)} -> ${filenamesPrint(r.extraBaseLocations)}")
                    }
                }
            }
        }

        fun printStats(
            out: PrintWriter,
            baseName: String,
            otherName: String,
        ) {
            out.println()

            out.println("Extra files in $baseName archive: ${unmatchedBaseExtras.size}")
            out.println("Extra files in $otherName archive: ${unmatchedOtherExtras.size}")
            out.println("Total files present in both archives: ${allBaseFiles.size - unmatchedBaseExtras.size}")

            out.println()
        }

        private fun filenamesPrint(filenames: List<String>): String =
            if (filenames.size == 1) {
                filenames[0]
            } else {
                "{${filenames.joinToString(", ")}}"
            }
    }
}
