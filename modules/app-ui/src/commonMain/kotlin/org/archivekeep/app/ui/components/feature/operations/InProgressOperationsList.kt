package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.ui.components.base.layout.keepSize
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.utils.toUiString
import org.archivekeep.files.procedures.progress.CopyOperationProgress
import org.archivekeep.utils.procedures.operations.OperationProgress
import kotlin.math.floor

@Composable
fun InProgressOperationsList(progress: List<OperationProgress>) {
    ProgressRowList(Modifier.keepSize()) {
        progress.forEach { atom ->
            val tags =
                listOfNotNull(
                    "${floor((atom.completion * 100)).toInt()}%",
                    atom.velocity?.toString(),
                    atom.timeEstimated?.toUiString(),
                )

            val tagsString =
                if (tags.isNotEmpty()) {
                    " [${tags.joinToString(", ")}]"
                } else {
                    null
                }

            val name =
                when (atom) {
                    is CopyOperationProgress -> atom.filename
                    else -> atom.javaClass.name
                }

            ProgressRow(
                progress = { atom.completion },
                text = "$name$tagsString",
            )
        }
    }
}
