package org.archivekeep.files.procedures.verify

data class VerifyProgress(
    val filesToVerify: Set<String>,
    val verifiedFiles: Set<String>,
    val failedFiles: Map<String, String>,
    val finished: Boolean,
)
