package org.archivekeep.app.core.domain.storages

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
        val physicalID: String,
        val driveType: DriveType,
    ) : StorageInformation {
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
}
