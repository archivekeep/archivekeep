package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.utils.identifiers.StorageURI

data class DemoPhysicalMedium(
    // this will probably differ in Linux and Windows
    // udevadm info --query=all /dev/nvme0n1 | grep ID_SERIAL=
    val physicalID: String,
    val driveType: StorageInformation.Partition.DriveType,
    val displayName: String,
    val isLocal: Boolean,
    val connectionStatus: Storage.ConnectionStatus,
    val id: String = displayName.toSlug(),
    val repositories: List<DemoRepository>,
) {
    val uri: StorageURI
        get() = StorageURI(DemoRepositoryURIData.ID, id)

    val reference = StorageNamedReference(uri, displayName)
}
