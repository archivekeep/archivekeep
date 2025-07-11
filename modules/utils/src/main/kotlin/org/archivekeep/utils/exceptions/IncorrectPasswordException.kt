package org.archivekeep.utils.exceptions

class IncorrectPasswordException(
    cause: Throwable,
) : RuntimeException(cause)
