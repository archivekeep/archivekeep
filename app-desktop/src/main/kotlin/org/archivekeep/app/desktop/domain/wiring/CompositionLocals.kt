package org.archivekeep.app.desktop.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.add.AddOperationSupervisorService
import org.archivekeep.app.core.operations.addpush.AddAndPushOperationService
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.desktop.domain.services.RepositoryOpenService

val LocalStorageService = staticCompositionLocalOfNotProvided<StorageService>()
val LocalRepoService = staticCompositionLocalOfNotProvided<RepositoryService>()
val LocalArchiveService = staticCompositionLocalOfNotProvided<ArchiveService>()

val LocalWalletDataStore = staticCompositionLocalOfNotProvided<JoseStorage<Credentials>>()

val LocalRepoToRepoSyncService = staticCompositionLocalOfNotProvided<RepoToRepoSyncService>()
val LocalAddPushService = staticCompositionLocalOfNotProvided<AddAndPushOperationService>()
val LocalAddOperationSupervisorService = staticCompositionLocalOfNotProvided<AddOperationSupervisorService>()

val LocalRegistry = staticCompositionLocalOfNotProvided<RegistryDataStore>()

val LocalFileStores = staticCompositionLocalOfNotProvided<FileStores>()

val LocalStorageRegistry = staticCompositionLocalOfNotProvided<StorageRegistry>()

val LocalOperationFactory = staticCompositionLocalOfNotProvided<OperationFactory>()

val LocalRepositoryOpenService = staticCompositionLocalOfNotProvided<RepositoryOpenService>()

val LocalComposeWindow = staticCompositionLocalOfNotProvided<ComposeWindow>()

inline fun <reified T> staticCompositionLocalOfNotProvided() =
    staticCompositionLocalOf<T> {
        error("CompositionLocal ${T::class.qualifiedName} not provided")
    }
