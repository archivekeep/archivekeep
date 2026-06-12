package org.archivekeep.app.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorageService
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureService
import org.archivekeep.app.core.procedures.deletedcleanup.DeletedFilesCleanupProcedureSupervisorService
import org.archivekeep.app.core.procedures.reindex.FileReindexProcedureSupervisorService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore

interface CoreApplicationServices {
    val scope: CoroutineScope
    val serviceWorkDispatcher: CoroutineDispatcher

    val registry: RegistryDataStore
    val repositoryIndexMemory: MemorizedRepositoryIndexRepository
    val repositoryMetadataMemory: MemorizedRepositoryMetadataRepository

    val walletDataStore: ProtectedDataStore<WalletPO>
    val credentialsStore: CredentialsStore

    val fileStores: FileStores

    val storageDrivers: Map<String, StorageDriver>

    val storageRegistry: StorageRegistry

    val archiveService: ArchiveService
    val storageService: StorageService
    val repositoryService: RepositoryService

    val syncService: RepoToRepoSyncService
    val addPushService: AddAndPushProcedureService
    val addOperationSupervisorService: IndexUpdateProcedureSupervisorService
    val deletedFilesCleanupProcedureSupervisorService: DeletedFilesCleanupProcedureSupervisorService
    val fileReindexProcedureSupervisorService: FileReindexProcedureSupervisorService

    val knownStorageService: KnownStorageService
}
