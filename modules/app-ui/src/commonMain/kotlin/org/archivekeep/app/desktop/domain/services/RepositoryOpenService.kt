package org.archivekeep.app.desktop.domain.services

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.ApplicationServices

interface RepositoryOpenService {
    fun getRepositoryOpener(uri: RepositoryURI): Flow<OptionalLoadable<() -> Unit>>
}

expect fun createRepositoryOpenService(applicationServices: ApplicationServices): RepositoryOpenService
