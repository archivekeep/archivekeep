package org.archivekeep.app.desktop.ui.components.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepositoryOpenService
import org.archivekeep.app.desktop.utils.collectAsState

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
