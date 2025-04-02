package org.archivekeep.app.desktop.domain.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineDispatcher

val LocalSharingCoroutineDispatcher =
    staticCompositionLocalOf<CoroutineDispatcher> {
        error("CompositionLocal LocalSharingCoroutineDispatcher not provided")
    }

val SharingCoroutineDispatcher: CoroutineDispatcher
    @Composable
    get() = LocalSharingCoroutineDispatcher.current
