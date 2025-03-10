package org.archivekeep.app.core.utils.exceptions

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

class RepositoryLockedException(
    val uri: RepositoryURI,
) : RuntimeException("Repository $uri is locked")
