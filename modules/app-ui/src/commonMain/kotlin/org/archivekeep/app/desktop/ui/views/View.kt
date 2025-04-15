package org.archivekeep.app.desktop.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope

interface View<VM> {
    @Composable
    fun produceViewModel(scope: CoroutineScope): VM

    @Composable
    fun render(
        modifier: Modifier,
        vm: VM,
    )
}
