package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Circle
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.utils.loading.Loadable

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
