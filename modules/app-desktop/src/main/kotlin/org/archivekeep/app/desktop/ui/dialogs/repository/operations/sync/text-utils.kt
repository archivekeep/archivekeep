package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.ui.utils.appendBoldStrikeThroughSpan
import org.archivekeep.files.operations.sync.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.sync.NewFilesSyncStep
import org.archivekeep.files.operations.sync.PreparedSyncOperation
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep

fun describePreparedSyncOperation(a: PreparedSyncOperation) =
    if (a.steps.isEmpty()) {
        "Nothing to do"
    } else {
        a.steps.joinToString("\n") {
            when (it) {
                is AdditiveRelocationsSyncStep -> "Duplicate ${it.subOperations.size} files."

                is RelocationsMoveApplySyncStep -> {
                    var text = "Move ${it.subOperations.size} files."

                    if (it.toIgnore.isNotEmpty()) {
                        text += "Ignore ${it.toIgnore.size} relocations."
                    }

                    text
                }

                is NewFilesSyncStep -> "Upload ${it.subOperations.size} files."
            }
        }
    }

fun AdditiveRelocationsSyncStep.AdditiveReplicationSubOperation.describe() =
    with(relocation) {
        "duplicate ${relocation.baseFilenames} to ${relocation.extraBaseLocations}"
    }

fun RelocationsMoveApplySyncStep.RelocationApplySubOperation.describe() =
    buildAnnotatedString {
        with(relocation) {
            if (isIncreasingDuplicates) {
                append("duplicate ")
                appendTransformingList(otherFilenames, crossNotIn(baseFilenames))
                append(" to ")
                appendTransformingList(baseFilenames, boldNotIn(otherFilenames))
            } else if (isDecreasingDuplicates) {
                append("deduplicate ")
                appendTransformingList(otherFilenames, crossNotIn(baseFilenames))
                append(" to keep only ")
                appendTransformingList(baseFilenames, boldNotIn(otherFilenames))
            } else {
                append("move ")
                appendTransformingList(extraOtherLocations, crossNotIn(extraBaseLocations))
                append(" to ")
                appendTransformingList(extraBaseLocations, boldNotIn(extraOtherLocations))
            }
        }
    }

fun boldNotIn(other: Collection<String>): AnnotatedString.Builder.(s: String) -> Unit {
    val s = other.toSet()

    return {
        if (it in s) {
            append(it)
        } else {
            appendBoldSpan(it)
        }
    }
}

fun crossNotIn(other: Collection<String>): AnnotatedString.Builder.(s: String) -> Unit {
    val s = other.toSet()

    return {
        if (it in s) {
            append(it)
        } else {
            appendBoldStrikeThroughSpan(it)
        }
    }
}

inline fun AnnotatedString.Builder.appendTransformingList(
    texts: List<String>,
    appendItem: AnnotatedString.Builder.(s: String) -> Unit,
) {
    texts.forEachIndexed { idx, it ->
        if (idx > 0) {
            append(", ")
        }
        appendItem(it)
    }
}
