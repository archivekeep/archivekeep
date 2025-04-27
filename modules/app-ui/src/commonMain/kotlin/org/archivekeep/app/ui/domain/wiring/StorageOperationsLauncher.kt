package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import org.archivekeep.app.core.utils.identifiers.StorageURI

data class StorageOperationsLaunchers(
    val openRename: (storageURI: StorageURI) -> Unit,
    val openMarkAsLocal: (storageURI: StorageURI) -> Unit,
    val openMarkAsExternal: (storageURI: StorageURI) -> Unit,
)

val LocalStorageOperationsLaunchers =
    staticCompositionLocalOf {
        StorageOperationsLaunchers(
            openRename = { invalidUseOfContext("openRename") },
            openMarkAsLocal = { invalidUseOfContext("openMarkAsLocal") },
            openMarkAsExternal = { invalidUseOfContext("openMarkAsExternal") },
        )
    }

private fun invalidUseOfContext(name: String): Nothing = throw Error("Context must be present to call $name")
