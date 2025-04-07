package org.archivekeep.app.desktop.domain.services

import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.desktop.domain.wiring.ApplicationServices

actual fun createRepositoryOpenService(applicationServices: ApplicationServices): RepositoryOpenService =
    DesktopRepositoryOpenService(
        applicationServices
            .environment
            .storageDrivers
            .values
            .filterIsInstance<FileSystemStorageDriver>()
            .firstOrNull(),
    )
