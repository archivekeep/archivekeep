package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Circle
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.utils.Loadable

@Composable
fun <T> DialogOverlayWithLoadableGuard(
    loadable: Loadable<T>,
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.(value: T) -> Unit,
) {
    DialogOverlay(onDismissRequest) {
        LoadableGuard(
            loadable,
            loadingContent = @Composable { TablerIcons.Circle },
        ) { loadedValue ->
            content(loadedValue)
        }
    }
}
