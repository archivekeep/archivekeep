package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.appendBoldStrikeThroughSpan
import org.archivekeep.files.procedures.sync.discovery.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSync
import org.archivekeep.files.procedures.sync.operations.AdditiveReplicationOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation

fun describePreparedSyncOperation(a: DiscoveredSync) =
    if (a.groups.isEmpty()) {
        "Nothing to do"
    } else {
        a.groups.joinToString("\n") {
            when (it) {
                is DiscoveredAdditiveRelocationsGroup -> "Duplicate ${it.operations.size} files."

                is DiscoveredRelocationsMoveApplyGroup -> {
                    var text = "Move ${it.operations.size} files."

                    if (it.toIgnore.isNotEmpty()) {
                        text += "Ignore ${it.toIgnore.size} relocations."
                    }

                    text
                }

                is DiscoveredNewFilesGroup -> "Upload ${it.operations.size} files."
            }
        }
    }

fun AdditiveReplicationOperation.describe() =
    with(relocation) {
        "duplicate $baseFilenames to $extraBaseLocations"
    }

fun RelocationApplyOperation.describe() =
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
