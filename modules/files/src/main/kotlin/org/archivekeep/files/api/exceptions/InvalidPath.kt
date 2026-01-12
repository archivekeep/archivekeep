package org.archivekeep.files.api.exceptions

sealed class InvalidPath(
    val path: String,
    message: String = "Path '$path' is not valid",
) : RuntimeException(message)

class MaliciousPath(
    path: String,
    message: String = "Path '$path' is malicious",
) : InvalidPath(path, message)

class NotNormalizedPath(
    path: String,
    message: String = "Path '$path' is not normalized",
) : InvalidPath(path, message)

class NotRegularFilePath(
    path: String,
    message: String = "Path '$path' is not a regular file",
) : InvalidPath(path, message)
