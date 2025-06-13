package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.ui.domain.services.RepositoryOpenService

val LocalApplicationServices = staticCompositionLocalOfNotProvided<ApplicationServices>()

val LocalStorageService = staticCompositionLocalOfNotProvided<StorageService>()
val LocalRepoService = staticCompositionLocalOfNotProvided<RepositoryService>()
val LocalArchiveService = staticCompositionLocalOfNotProvided<ArchiveService>()

val LocalRepoToRepoSyncService = staticCompositionLocalOfNotProvided<RepoToRepoSyncService>()
val LocalAddPushService = staticCompositionLocalOfNotProvided<AddAndPushProcedureService>()
val LocalIndexUpdateProcedureSupervisorService = staticCompositionLocalOfNotProvided<IndexUpdateProcedureSupervisorService>()

val LocalRegistry = staticCompositionLocalOfNotProvided<RegistryDataStore>()

val LocalFileStores = staticCompositionLocalOfNotProvided<FileStores>()

val LocalStorageRegistry = staticCompositionLocalOfNotProvided<StorageRegistry>()

val LocalOperationFactory = staticCompositionLocalOfNotProvided<OperationFactory>()

val LocalRepositoryOpenService = staticCompositionLocalOfNotProvided<RepositoryOpenService>()

inline fun <reified T> staticCompositionLocalOfNotProvided() =
    staticCompositionLocalOf<T> {
        error("CompositionLocal ${T::class.qualifiedName} not provided")
    }
