package org.archivekeep.app.ui.domain.services

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices

val SharingCoroutineDispatcher: CoroutineDispatcher
    @Composable
    get() = LocalApplicationServices.current.serviceWorkDispatcher
