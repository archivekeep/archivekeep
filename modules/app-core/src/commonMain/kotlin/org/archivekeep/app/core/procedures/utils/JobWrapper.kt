package org.archivekeep.app.core.procedures.utils

import kotlinx.coroutines.flow.Flow

interface JobWrapper<S> {
    val state: Flow<S>

    fun cancel()
}
