package org.archivekeep.files.procedures.verify

interface VerifyProgressTracker {
    fun onFileVerified(verifiedFile: String)

    fun onFileVerificationFailed(
        failedFile: String,
        reason: String,
    )
}
