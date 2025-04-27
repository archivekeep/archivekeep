package org.archivekeep.app.ui.domain.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage

fun JoseStorage<Credentials>.canUnlockFlow() = autoloadFlow.map { it is JoseStorage.State.Locked }

@Composable
fun JoseStorage<Credentials>.canUnlock() =
    autoloadFlow
        .collectAsState()
        .value is JoseStorage.State.Locked
