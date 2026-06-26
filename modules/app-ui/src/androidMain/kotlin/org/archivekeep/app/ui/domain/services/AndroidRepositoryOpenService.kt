package org.archivekeep.app.ui.domain.services

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.loading.optional.OptionalLoadable

@Inject
@SingleIn(AppScope::class)
class AndroidRepositoryOpenService : RepositoryOpenService {
    override fun getRepositoryOpener(uri: RepositoryURI): Flow<OptionalLoadable<() -> Unit>> {
        // TODO: use android intents
        return flowOf(OptionalLoadable.NotAvailable())
    }
}
