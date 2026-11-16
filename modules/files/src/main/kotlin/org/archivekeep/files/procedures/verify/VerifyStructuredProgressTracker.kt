package org.archivekeep.files.procedures.verify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VerifyStructuredProgressTracker(
    filesToVerify: Set<String>,
) : VerifyProgressTracker {
    private val verifyProgressMutableFlow =
        MutableStateFlow(
            VerifyProgress(
                filesToVerify,
                emptySet(),
                emptyMap(),
                false,
            ),
        )

    val verifyProgressFlow = verifyProgressMutableFlow.asStateFlow()

    override fun onFileVerified(verifiedFile: String) {
        verifyProgressMutableFlow.update {
            it.copy(
                verifiedFiles = it.verifiedFiles + setOf(verifiedFile),
            )
        }
    }

    override fun onFileVerificationFailed(
        failedFile: String,
        reason: String,
    ) {
        verifyProgressMutableFlow.update {
            it.copy(
                failedFiles = it.failedFiles + mapOf(failedFile to reason),
            )
        }
    }
}
