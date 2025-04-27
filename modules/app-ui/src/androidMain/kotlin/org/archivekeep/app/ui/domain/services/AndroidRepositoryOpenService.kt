package org.archivekeep.app.ui.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

class AndroidRepositoryOpenService : RepositoryOpenService {
    override fun getRepositoryOpener(uri: RepositoryURI): Flow<OptionalLoadable<() -> Unit>> {
        // TODO: use android intents
        return flowOf(OptionalLoadable.NotAvailable())
    }
}
