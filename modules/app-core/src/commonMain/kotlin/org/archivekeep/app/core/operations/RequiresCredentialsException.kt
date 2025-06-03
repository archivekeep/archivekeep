package org.archivekeep.app.core.operations

class RequiresCredentialsException(
    message: String = "Credentials are required to connect",
) : Exception(message)
