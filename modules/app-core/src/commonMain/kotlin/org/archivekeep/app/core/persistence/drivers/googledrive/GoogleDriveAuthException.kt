package org.archivekeep.app.core.persistence.drivers.googledrive

open class GoogleDriveAuthException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
