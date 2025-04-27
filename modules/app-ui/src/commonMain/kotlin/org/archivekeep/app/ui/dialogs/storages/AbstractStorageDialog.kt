package org.archivekeep.app.ui.dialogs.storages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCard
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.utils.loading.Loadable

abstract class AbstractStorageDialog<T_State : AbstractStorageDialog.IState, T_VM : Any>(
    val uri: StorageURI,
) : Dialog {
    interface IState {
        val title: AnnotatedString
    }

    @Composable
    abstract fun rememberVM(
        scope: CoroutineScope,
        storage: Storage,
        onClose: () -> Unit,
    ): T_VM

    @Composable
    protected abstract fun rememberState(vm: T_VM): Loadable<T_State>

    @Composable
    abstract fun renderContent(state: T_State)

    @Composable
    abstract fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: T_State,
    )

    @Composable
    fun renderDialogContents(
        state: T_State,
        onClose: () -> Unit,
    ) {
        DialogInnerContainer(
            remember {
                buildAnnotatedString {
                    append(state.title)
                }
            },
            content = { renderContent(state) },
            bottomContent = {
                DialogButtonContainer {
                    renderButtons(onClose, state)
                }
            },
        )
    }

    @Composable
    internal fun renderDialogCardForPreview(state: T_State) {
        DialogCard { renderDialogContents(state, onClose = {}) }
    }

    @Composable
    override fun render(onClose: () -> Unit) {
        val storageService = LocalStorageService.current

        val storage =
            remember(storageService, uri) {
                storageService.storage(uri)
            }

        DialogOverlayCard(onDismissRequest = onClose) {
            val vm = rememberVM(rememberCoroutineScope(), storage, onClose)

            LoadableGuard(rememberState(vm)) { state ->
                renderDialogContents(state, onClose)
            }
        }
    }
}
