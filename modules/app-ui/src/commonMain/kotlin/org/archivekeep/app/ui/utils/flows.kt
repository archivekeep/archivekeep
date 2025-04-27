package org.archivekeep.app.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile

fun <T> (Flow<T>).stickToFirstNotNull() =
    this
        .transformWhile {
            emit(it)
            it == null
        }

fun <T> (StateFlow<T>).stickToFirstNotNullAsState(scope: CoroutineScope): StateFlow<T?> =
    this.stickToFirstNotNull().stateIn(scope, SharingStarted.Lazily, this.value)
