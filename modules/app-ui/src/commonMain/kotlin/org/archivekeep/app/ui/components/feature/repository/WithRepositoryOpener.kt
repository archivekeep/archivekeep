package org.archivekeep.app.ui.components.feature.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.collectAsState
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

data class RepositoryOpenerScope(
    val openRepository: (() -> Unit)?,
)

@Composable
fun <R> withRepositoryOpener(
    uri: RepositoryURI,
    factory: RepositoryOpenerScope.() -> R,
): Loadable<R> {
    val repositoryOpener = LocalApplicationServices.current.repositoryOpenService

    val openFunction = remember(repositoryOpener, uri) { repositoryOpener.getRepositoryOpener(uri) }.collectAsState().value

    return when (openFunction) {
        is OptionalLoadable.Loading -> {
            Loadable.Loading
        }

        is OptionalLoadable.Failed -> {
            Loadable.Failed(openFunction.cause)
        }

        is OptionalLoadable.NotAvailable -> {
            with(
                RepositoryOpenerScope(openRepository = null),
            ) {
                Loadable.Loaded(factory())
            }
        }

        is OptionalLoadable.LoadedAvailable -> {
            with(
                RepositoryOpenerScope(openRepository = openFunction.value),
            ) {
                Loadable.Loaded(factory())
            }
        }
    }
}
