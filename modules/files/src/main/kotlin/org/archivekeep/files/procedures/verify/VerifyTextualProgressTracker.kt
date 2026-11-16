package org.archivekeep.files.procedures.verify

import java.io.PrintWriter
import java.nio.file.Path

class VerifyTextualProgressTracker(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) : VerifyProgressTracker {
    override fun onFileVerified(verifiedFile: String) {
        out.println("verified: ${fromArchiveToRelativePath(verifiedFile)}")
    }

    override fun onFileVerificationFailed(
        failedFile: String,
        reason: String,
    ) {
        out.println("verification failed: ${fromArchiveToRelativePath(failedFile)} - $reason")
    }
}
