package org.archivekeep.app.ui.utils

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver

@BindingContainer
class FilesystemDriverContainer {
    @Provides
    @SingleIn(AppScope::class)
    @ElementsIntoSet
    fun provideStorageDrivers(
        scope: CoroutineScope,
        fileStores: FileStores,
        credentialsStore: CredentialsStore,
    ): List<StorageDriver> = listOf(FileSystemStorageDriver(scope, fileStores, credentialsStore))
}
