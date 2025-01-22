package org.archivekeep.app.desktop.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.utils.Loadable

@Composable
fun <T> LoadableGuard(
    loadable: Loadable<T>,
    loadingContent: @Composable () -> Unit = { Text("Loading") },
    content: @Composable (value: T) -> Unit,
) {
    when (loadable) {
        is Loadable.Failed ->
            Text("Failed: ${loadable.throwable.message}")

        is Loadable.Loading ->
            loadingContent()

        is Loadable.Loaded ->
            content(loadable.value)
    }
}

@Composable
fun <T1, T2> LoadableGuard(
    loadable1: Loadable<T1>,
    loadable2: Loadable<T2>,
    content: @Composable (value1: T1, value2: T2) -> Unit,
) {
    val value1: T1
    val value2: T2

    when (loadable1) {
        is Loadable.Failed -> {
            Text("Failed")
            return
        }
        is Loadable.Loading -> {
            Text("Loading")
            return
        }
        is Loadable.Loaded ->
            value1 = loadable1.value
    }

    when (loadable2) {
        is Loadable.Failed -> {
            Text("Failed")
            return
        }
        is Loadable.Loading -> {
            Text("Loading")
            return
        }
        is Loadable.Loaded ->
            value2 = loadable2.value
    }

    content(value1, value2)
}
