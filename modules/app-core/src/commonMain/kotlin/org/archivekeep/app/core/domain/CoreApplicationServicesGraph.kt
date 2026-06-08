package org.archivekeep.app.core.domain

import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver

interface CoreApplicationServicesGraph : CoreApplicationServices {
    @Provides
    @ElementsIntoSet
    fun provideDefaultStorageDrivers(
        scope: CoroutineScope,
        credentialsStore: CredentialsStore,
    ): List<StorageDriver> =
        (
            listOf(
                GRPCStorageDriver(scope, credentialsStore),
                S3StorageDriver(scope, credentialsStore),
            )
        )

    @Provides
    fun provideStorageDriversMap(storageDriverSet: Set<StorageDriver>): Map<String, StorageDriver> = storageDriverSet.associateBy { it.ID }
}
