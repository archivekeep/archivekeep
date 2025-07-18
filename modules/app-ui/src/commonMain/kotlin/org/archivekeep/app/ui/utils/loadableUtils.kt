package org.archivekeep.app.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

@Composable
fun <T> Flow<T>.collectAsLoadable(): Loadable<T> = collectAsLoadableState().value

@Composable
fun <T> Flow<T>.collectAsLoadableState(): State<Loadable<T>> =
    remember(this) {
        mapToLoadable().onEach(::log)
    }.collectLoadableFlowAsState()

@Composable
fun <T> Flow<Loadable<T>>.collectLoadableFlowAsState(): State<Loadable<T>> = this.collectAsState(Loadable.Loading)

@Composable
fun <T> Flow<Loadable<T>>.collectLoadableFlow(): Loadable<T> =
    remember(this) {
        this.onEach(::log)
    }.collectAsState(
        Loadable.Loading,
    ).value

fun <T> log(loadable: Loadable<T>) {
    if (loadable is Loadable.Failed) {
        println("ERROR: Loadable collection failed: ${loadable.throwable}")
        loadable.throwable.printStackTrace()
    }
}

@Composable
fun <T> Flow<OptionalLoadable<T>>.collectAsState() =
    remember(this) {
        this.onEach(::log)
    }.collectAsState(OptionalLoadable.Loading)

private fun <T> log(loadable: OptionalLoadable<T>) {
    if (loadable is OptionalLoadable.Failed) {
        println("ERROR: Loadable collection failed: ${loadable.cause}")
        loadable.cause.printStackTrace()
    }
}
