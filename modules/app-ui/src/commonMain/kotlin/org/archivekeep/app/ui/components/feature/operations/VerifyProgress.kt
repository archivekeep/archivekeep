package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.procedures.verify.VerifySupervisor
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.utils.filesAutoPluralTranslated
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_verify_failed
import org.archivekeep.ui.resources.dialog_verify_title
import org.archivekeep.ui.resources.dialog_verify_verified
import org.jetbrains.compose.resources.stringResource

@Composable
fun VerifyProgress(jobState: VerifySupervisor.JobState) {
    LabelText(stringResource(Res.string.dialog_verify_title))

    ProgressRowList {
        val filesToVerify = jobState.verifyProgress.filesToVerify
        if (filesToVerify.isNotEmpty()) {
            ProgressRow(progress = {
                jobState.verifyProgress.verifiedFiles.size / filesToVerify.size.toFloat()
            }, stringResource(Res.string.dialog_verify_verified, jobState.verifyProgress.verifiedFiles.size, filesAutoPluralTranslated(filesToVerify)))
        }
        if (jobState.verifyProgress.failedFiles.isNotEmpty()) {
            ProgressRow(progress = {
                jobState.verifyProgress.failedFiles.size / filesToVerify.size.toFloat()
            }, stringResource(Res.string.dialog_verify_failed, jobState.verifyProgress.failedFiles.size, filesAutoPluralTranslated(filesToVerify)))
        }
    }
}
