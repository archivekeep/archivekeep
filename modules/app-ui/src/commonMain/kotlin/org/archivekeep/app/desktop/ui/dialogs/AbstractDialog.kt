package org.archivekeep.app.desktop.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayWithLoadableGuard
import org.archivekeep.utils.loading.Loadable

abstract class AbstractDialog<T_State : AbstractDialog.IState, T_VM : AbstractDialog.IVM> : Dialog {
    interface IState {
        val title: AnnotatedString
    }

    interface IVM {
        fun onClose()
    }

    @Composable
    abstract fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): T_VM

    @Composable
    protected abstract fun rememberState(vm: T_VM): Loadable<T_State>

    @Composable
    protected abstract fun ColumnScope.renderContent(state: T_State)

    @Composable
    protected abstract fun RowScope.renderButtons(state: T_State)

    @Composable
    fun renderDialogCard(state: T_State) {
        DialogCardWithDialogInnerContainer(
            state.title,
            content = {
                renderContent(state)
            },
            bottomContent = {
                DialogButtonContainer {
                    renderButtons(state)
                }
            },
        )
    }

    @Composable
    override fun render(onClose: () -> Unit) {
        val coroutineScope = rememberCoroutineScope()

        val vm = rememberVM(coroutineScope, onClose)

        DialogOverlayWithLoadableGuard(
            rememberState(vm),
            onDismissRequest = vm::onClose,
        ) { state ->
            renderDialogCard(state)
        }
    }
}
