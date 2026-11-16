package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.appendBoldStrikeThroughSpan
import org.archivekeep.files.procedures.sync.discovery.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSync
import org.archivekeep.files.procedures.sync.operations.AdditiveReplicationOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_sync_deduplicate_connector
import org.archivekeep.ui.resources.dialog_sync_deduplicate_prefix
import org.archivekeep.ui.resources.dialog_sync_duplicate_base_to_extra
import org.archivekeep.ui.resources.dialog_sync_duplicate_files
import org.archivekeep.ui.resources.dialog_sync_duplicate_prefix
import org.archivekeep.ui.resources.dialog_sync_ignore_relocations
import org.archivekeep.ui.resources.dialog_sync_move_files
import org.archivekeep.ui.resources.dialog_sync_move_prefix
import org.archivekeep.ui.resources.dialog_sync_nothing_to_do
import org.archivekeep.ui.resources.dialog_sync_upload_files
import org.jetbrains.compose.resources.getString

suspend fun describePreparedSyncOperation(a: DiscoveredSync) =
    if (a.groups.isEmpty()) {
        getString(Res.string.dialog_sync_nothing_to_do)
    } else {
        buildString {
            a.groups.forEachIndexed { index, group ->
                if (index > 0) append("\n")
                when (group) {
                    is DiscoveredAdditiveRelocationsGroup -> {
                        append(getString(Res.string.dialog_sync_duplicate_files, group.operations.size))
                    }
                    is DiscoveredRelocationsMoveApplyGroup -> {
                        append(getString(Res.string.dialog_sync_move_files, group.operations.size))
                        if (group.toIgnore.isNotEmpty()) {
                            append(" ")
                            append(getString(Res.string.dialog_sync_ignore_relocations, group.toIgnore.size))
                        }
                    }
                    is DiscoveredNewFilesGroup -> {
                        append(getString(Res.string.dialog_sync_upload_files, group.operations.size))
                    }
                }
            }
        }
    }

fun AdditiveReplicationOperation.describe() =
    runBlocking {
        with(relocation) {
            getString(Res.string.dialog_sync_duplicate_base_to_extra, baseFilenames, extraBaseLocations)
        }
    }

fun RelocationApplyOperation.describe() =
    runBlocking {
        buildAnnotatedString {
            with(relocation) {
                if (isIncreasingDuplicates) {
                    append(getString(Res.string.dialog_sync_duplicate_prefix))
                    appendTransformingList(otherFilenames, crossNotIn(baseFilenames))
                    append(" to ")
                    appendTransformingList(baseFilenames, boldNotIn(otherFilenames))
                } else if (isDecreasingDuplicates) {
                    append(getString(Res.string.dialog_sync_deduplicate_prefix))
                    appendTransformingList(otherFilenames, crossNotIn(baseFilenames))
                    append(getString(Res.string.dialog_sync_deduplicate_connector))
                    appendTransformingList(baseFilenames, boldNotIn(otherFilenames))
                } else {
                    append(getString(Res.string.dialog_sync_move_prefix))
                    appendTransformingList(extraOtherLocations, crossNotIn(extraBaseLocations))
                    append(" to ")
                    appendTransformingList(extraBaseLocations, boldNotIn(extraOtherLocations))
                }
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
