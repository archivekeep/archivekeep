package org.archivekeep.app.desktop.ui.dialogs.sync

import org.archivekeep.files.operations.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.NewFilesSyncStep
import org.archivekeep.files.operations.PreparedSyncOperation
import org.archivekeep.files.operations.RelocationsMoveApplySyncStep

fun describePreparedSyncOperationWithDetails(
    a: PreparedSyncOperation,
    action: String = "Add",
) = if (a.isNoOp()) {
    "NoOP"
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
                    listOf("Move ${it.relocations.size} files.") +
                        it.relocations.map { relocation ->
                            " - move from ${relocation.extraOtherLocations} to ${relocation.extraBaseLocations}"
                        }

                is NewFilesSyncStep ->
                    listOf("${action.capitalize()} ${it.unmatchedBaseExtras.size} files.") +
                        it.unmatchedBaseExtras.map { group ->
                            " - $action new ${group.filenames}"
                        }
            }
        }.joinToString("\n")
}
