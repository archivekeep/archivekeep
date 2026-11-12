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
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.info_function_ensure
import org.archivekeep.ui.resources.info_function_fingerprint
import org.archivekeep.ui.resources.info_function_integrity
import org.archivekeep.ui.resources.info_function_title
import org.archivekeep.ui.resources.info_function_verify
import org.jetbrains.compose.resources.stringResource

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
                stringResource(Res.string.info_function_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            val fingerprintText = stringResource(Res.string.info_function_fingerprint)
            val ensureText = stringResource(Res.string.info_function_ensure)
            val verifyText = stringResource(Res.string.info_function_verify)
            val integrityText = stringResource(Res.string.info_function_integrity)
            Text(
                buildAnnotatedString {
                    withStyle(ParagraphStyle(lineBreak = LineBreak.Paragraph)) {
                        appendLine(fingerprintText)
                    }
                    appendLine(ensureText)
                    appendLine(verifyText)
                    append(integrityText)
                },
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
        }
    }
}
