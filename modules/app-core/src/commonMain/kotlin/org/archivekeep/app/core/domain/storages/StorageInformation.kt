package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.generics.OptionalLoadable

sealed interface StorageInformation {
    /**
     * Physical medium storage is something, that you can carry with yourself,
     * and this its primary form of operation.
     *
     * It can be physically connected to one device.
     *
     * It can be HDD, SSD, FlashDrive, SDCard,... other Media
     */
    class Partition(
        val details: Flow<OptionalLoadable<Details>>,
    ) : StorageInformation {
        data class Details(
            val physicalID: String,
            val driveType: DriveType,
        )

        enum class DriveType {
            SSD,
            HDD,
            Other,
        }
    }

    /**
     * Online storage is meant to be available via network.
     *
     * It can be self-hosted, or hosted by someone else, or a service.
     *
     * By hosting methods:
     *
     * - NAS,
     * - SelfHosted on Premises,
     * - SelfHosted on Server,
     * - SelfHosted on Cloud,
     * - Hosted by a friend,
     * - Service hosted on server,
     * - Service hosted on cloud,
     * - other
     *
     * By communication types:
     *
     * - application,
     * - remote file system,
     * - object storage
     * - other
     */
    data object OnlineStorage : StorageInformation

    data object CompoundStorage : StorageInformation

    data class Error(
        val error: Throwable,
    ) : StorageInformation
}
