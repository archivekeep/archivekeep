package org.archivekeep.app.core.domain.repositories

data class UnlockOptions(
    // TODO - sort out mixed use for remember credentials and session (not credentials itself)
    // maybe move completely out from unlock options, to higher level (possibility to control location of storage)
    val rememberSession: Boolean = true,
)
