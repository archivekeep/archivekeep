package org.archivekeep.app.desktop.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.addpush.AddPushOperationService
import org.archivekeep.app.core.operations.derived.SyncService
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore

val LocalStorageService =
    staticCompositionLocalOf<StorageService> {
        error("CompositionLocal StorageService not provided")
    }

val LocalRepoService =
    staticCompositionLocalOf<RepositoryService> {
        error("CompositionLocal IRepositoryService not provided")
    }

val LocalArchiveService =
    staticCompositionLocalOf<ArchiveService> {
        error("CompositionLocal ArchiveService not provided")
    }

val LocalWalletDataStore =
    staticCompositionLocalOf<JoseStorage<Credentials>> {
        error("CompositionLocal JoseStorage not provided")
    }

val LocalSyncService =
    staticCompositionLocalOf<SyncService> {
        error("CompositionLocal SyncService not provided")
    }

val LocalAddPushService =
    staticCompositionLocalOf<AddPushOperationService> {
        error("CompositionLocal AddPushOperationService not provided")
    }

val LocalRegistry =
    staticCompositionLocalOf<RegistryDataStore> {
        error("CompositionLocal Registry not provided")
    }

val LocalFileStores =
    staticCompositionLocalOf<FileStores> {
        error("CompositionLocal FileStores not provided")
    }

val LocalStorageRegistry =
    staticCompositionLocalOf<StorageRegistry> {
        error("CompositionLocal StorageRegistry not provided")
    }

val LocalOperationFactory =
    staticCompositionLocalOf<OperationFactory> {
        error("CompositionLocal OperationFactory not provided")
    }
