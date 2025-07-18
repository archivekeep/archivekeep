package org.archivekeep.app.ui.domain.services

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.utils.loading.optional.OptionalLoadable

interface RepositoryOpenService {
    fun getRepositoryOpener(uri: RepositoryURI): Flow<OptionalLoadable<() -> Unit>>
}

expect fun createRepositoryOpenService(applicationServices: ApplicationServices): RepositoryOpenService
