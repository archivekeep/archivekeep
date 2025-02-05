package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard

@Composable
fun HomeArchiveKeepFunctionDescription() {
    SectionCard(
        Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "How ArchiveKeep functions?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                buildAnnotatedString {
                    withStyle(ParagraphStyle(lineBreak = LineBreak.Paragraph)) {
                        appendLine("Fingerprint is created for each file added to an archive. This is used to:")
                    }
                    appendLine("- ensure file remains unchanged and exactly as it was when you decided to archive it.")
                    appendLine("- verify file is not changed (corrupted), when it is being copied to other storage.")
                    append(
                        "- verify integrity of files in an archive and detect corruption, that needs to be sorted out. (this is WIP feature)",
                    )
                },
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
        }
    }
}
