package org.archivekeep.files.driver.googledrive

class GoogleDriveLocationNotInitializedAsRepositoryException : RuntimeException()

class GoogleDriveUnsupportedRepositoryTypeException(
    val repositoryType: String,
) : RuntimeException("Google Drive repository type '$repositoryType' is not supported")

class GoogleDriveAmbiguousFileException(
    val filename: String,
) : RuntimeException("Google Drive contains multiple files named '$filename' in the repository")
