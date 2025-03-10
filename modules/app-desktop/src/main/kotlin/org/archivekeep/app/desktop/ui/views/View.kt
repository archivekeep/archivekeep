package org.archivekeep.app.desktop.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope

interface View<S> {
    @Composable
    fun producePersistentState(scope: CoroutineScope): S

    @Composable
    fun render(
        modifier: Modifier,
        state: S,
    )
}
