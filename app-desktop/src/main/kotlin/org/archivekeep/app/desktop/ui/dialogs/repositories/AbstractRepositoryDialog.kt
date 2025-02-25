package org.archivekeep.app.desktop.ui.dialogs.repositories

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayWithLoadableGuard
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.AbstractRepositoryDialog.BaseState
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.Loadable

abstract class AbstractRepositoryDialog<T_State : BaseState, T_VM : AbstractRepositoryDialog.IVM<T_State>>(
    val uri: RepositoryURI,
) : Dialog {
    interface BaseState {
        val title: AnnotatedString
    }

    interface IVM<out T : BaseState> {
        val stateFlow: StateFlow<Loadable<T>>

        fun onClose()
    }

    @Composable
    abstract fun createVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): T_VM

    @Composable
    abstract fun renderContent(
        vm: T_VM,
        state: T_State,
    )

    @Composable
    abstract fun RowScope.renderButtons(vm: T_VM)

    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val repositoryService = LocalRepoService.current

        val coroutineScope = rememberCoroutineScope()

        val repository = remember(uri) { repositoryService.getRepository(uri) }

        val vm = createVM(coroutineScope, repository, onClose)

        DialogOverlayWithLoadableGuard(
            vm.stateFlow.collectLoadableFlow(),
            onDismissRequest = vm::onClose,
        ) { state ->
            DialogCardWithDialogInnerContainer(
                state.title,
                content = {
                    renderContent(vm, state)
                },
                bottomContent = {
                    DialogButtonContainer {
                        renderButtons(vm)
                    }
                },
            )
        }
    }
}
