package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import org.archivekeep.files.operations.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.NewFilesSyncStep
import org.archivekeep.files.operations.PreparedSyncOperation
import org.archivekeep.files.operations.RelocationsMoveApplySyncStep
import java.util.Locale

fun describePreparedSyncOperation(a: PreparedSyncOperation) =
    if (a.steps.isEmpty()) {
        "Nothing to do"
    } else {
        a.steps.joinToString("\n") {
            when (it) {
                is AdditiveRelocationsSyncStep -> "Duplicate ${it.relocations.size} files."

                is RelocationsMoveApplySyncStep -> {
                    var text = "Move ${it.toApply.size} files."

                    if (it.toIgnore.isNotEmpty()) {
                        text += "Ignore ${it.toIgnore.size} relocations."
                    }

                    text
                }

                is NewFilesSyncStep -> "Upload ${it.unmatchedBaseExtras.size} files."
            }
        }
    }

fun describePreparedSyncOperationWithDetails(
    a: PreparedSyncOperation,
    action: String = "Add",
) = if (a.steps.isEmpty()) {
    "Nothing to do"
} else {
    a.steps
        .flatMap {
            when (it) {
                is AdditiveRelocationsSyncStep ->
                    listOf("Duplicate ${it.relocations.size} files:") +
                        it.relocations.map { relocation ->
                            " - duplicate ${relocation.baseFilenames} to ${relocation.extraBaseLocations}"
                        }

                is RelocationsMoveApplySyncStep ->
                    (
                        if (it.toApply.isNotEmpty()) {
                            listOf("Execute ${it.toApply.size} relocations:")
                        } else {
                            emptyList()
                        }
                    ) + (
                        it.toApply.map { relocation ->
                            "- ${relocation.describe()}"
                        }
                    ) + (
                        if (it.toIgnore.isNotEmpty()) {
                            listOf("Ignore ${it.toIgnore.size} relocations:")
                        } else {
                            emptyList()
                        }
                    ) + (
                        it.toIgnore.map { relocation ->
                            " - ignore ${relocation.describe()}"
                        }
                    )

                is NewFilesSyncStep ->
                    listOf("${action.capitalize()} ${it.unmatchedBaseExtras.size} files:") +
                        it.unmatchedBaseExtras.map { group ->
                            " - $action new ${group.filenames}"
                        }
            }
        }.joinToString("\n")
}

private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun CompareOperation.Result.Relocation.describe() =
    (
        if (isIncreasingDuplicates) {
            "duplication increase from $otherFilenames to $baseFilenames"
        } else if (isDecreasingDuplicates) {
            "duplication decrease from $otherFilenames to $baseFilenames"
        } else {
            "move from $extraOtherLocations to $extraBaseLocations"
        }
    )
