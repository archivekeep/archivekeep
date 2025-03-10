package org.archivekeep.app.desktop.ui.dialogs.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog

abstract class AbstractRepositoryDialog<T_State : AbstractDialog.IState, T_VM : AbstractDialog.IVM>(
    val uri: RepositoryURI,
) : AbstractDialog<T_State, T_VM>() {
    @Composable
    abstract fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): T_VM

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): T_VM {
        val repositoryService = LocalRepoService.current

        val repository = remember(uri) { repositoryService.getRepository(uri) }

        return rememberVM(scope, repository, onClose)
    }
}
