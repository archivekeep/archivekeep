package org.archivekeep.app.ui.domain.services

import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.ui.domain.wiring.ApplicationServicesGraph

actual fun createRepositoryOpenService(applicationServices: ApplicationServicesGraph): RepositoryOpenService =
    DesktopRepositoryOpenService(
        applicationServices
            .storageDrivers
            .values
            .filterIsInstance<FileSystemStorageDriver>()
            .firstOrNull(),
    )
