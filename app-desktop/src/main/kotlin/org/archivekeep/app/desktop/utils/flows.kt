package org.archivekeep.app.desktop.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile

fun <T> (Flow<T>).stickToFirstNotNull(scope: CoroutineScope): StateFlow<T?> =
    this
        .transformWhile {
            emit(it)
            it == null
        }.stateIn(scope, SharingStarted.Lazily, null)
