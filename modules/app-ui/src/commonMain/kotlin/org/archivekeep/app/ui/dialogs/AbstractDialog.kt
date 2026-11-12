package org.archivekeep.app.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlayWithLoadableGuard
import org.archivekeep.app.ui.components.designsystem.dialog.defaultDialogWidthModifier
import org.archivekeep.utils.loading.Loadable

abstract class AbstractDialog<T_State : Any, T_VM : AbstractDialog.IVM> : Dialog {
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

    protected open fun dialogWidthModifier(state: T_State): Modifier = defaultDialogWidthModifier

    @Composable
    protected abstract fun title(state: T_State): AnnotatedString

    @Composable
    protected abstract fun ColumnScope.renderContent(state: T_State)

    @Composable
    protected abstract fun RowScope.renderButtons(state: T_State)

    @Composable
    override fun render(onClose: () -> Unit) {
        val coroutineScope = rememberCoroutineScope()

        val vm = rememberVM(coroutineScope, onClose)

        DialogOverlayWithLoadableGuard(
            rememberState(vm),
            onDismissRequest = vm::onClose,
        ) { state ->
            DialogCardWithDialogInnerContainer(
                title(state),
                widthModifier = dialogWidthModifier(state),
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
    }
}
