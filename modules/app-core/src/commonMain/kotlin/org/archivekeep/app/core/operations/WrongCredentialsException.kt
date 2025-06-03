package org.archivekeep.app.core.operations

class WrongCredentialsException(
    message: String = "Credentials provided don't work",
    cause: Throwable? = null,
) : Exception(message, cause)
