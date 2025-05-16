package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.operations.tasks.CopyProgressOperation
import org.archivekeep.files.operations.tasks.InProgressOperationStats
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
fun InProgressOperationsList(progress: List<InProgressOperationStats>) {
    ProgressRowList {
        progress.forEach { atom ->
            ProgressRow(
                progress = { atom.completion },
                text = when (atom) {
                    is CopyProgressOperation ->
                        "${(atom.completion * 100).toInt()}% ${atom.filename} [${atom.velocity}${atom.timeEstimated?.let { ", ${it.toUiString()}" }}]"

                    else ->
                        atom.javaClass.name
                }
            )
        }
    }
}

private fun (Duration).toUiString(): String =
    if (this < 1.minutes) {
        "${this.inWholeSeconds}s"
    } else {
        "${this.inWholeMinutes}m ${this.inWholeSeconds - this.inWholeMinutes * 60}s"
    }
