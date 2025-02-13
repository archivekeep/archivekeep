package org.archivekeep.app.desktop.domain.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage

@Composable
fun JoseStorage<Credentials>.canUnlock() =
    autoloadFlow
        .collectAsState()
        .value is JoseStorage.State.Locked
