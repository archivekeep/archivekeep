package org.archivekeep.app.ui.components.feature.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.domain.wiring.LocalRepositoryOpenService
import org.archivekeep.app.ui.utils.collectAsState
import org.archivekeep.utils.loading.optional.OptionalLoadable

data class RepositoryOpenerScope(
    val openRepository: () -> Unit,
)

@Composable
fun WithRepositoryOpener(
    uri: RepositoryURI,
    contentIfUnsupported: @Composable () -> Unit = {},
    content: @Composable RepositoryOpenerScope.() -> Unit,
) {
    val repositoryOpener = LocalRepositoryOpenService.current

    val openFunction = remember(repositoryOpener, uri) { repositoryOpener.getRepositoryOpener(uri) }.collectAsState().value

    when (openFunction) {
        is OptionalLoadable.Loading -> {}
        is OptionalLoadable.Failed -> {}
        is OptionalLoadable.NotAvailable -> contentIfUnsupported()
        is OptionalLoadable.LoadedAvailable -> {
            with(
                RepositoryOpenerScope(openRepository = openFunction.value),
            ) {
                content()
            }
        }
    }
}
