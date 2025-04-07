package org.archivekeep.app.desktop.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import java.awt.Desktop

class DesktopRepositoryOpenService(
    private val fileSystemStorageDriver: FileSystemStorageDriver?,
) : RepositoryOpenService {
    override fun getRepositoryOpener(uri: RepositoryURI): Flow<OptionalLoadable<() -> Unit>> =
        when (val uriData = uri.typedRepoURIData) {
            is FileSystemRepositoryURIData -> {
                if (!Desktop.isDesktopSupported() || fileSystemStorageDriver == null) {
                    flowOf(OptionalLoadable.NotAvailable())
                } else {
                    fileSystemStorageDriver
                        .getPathInFileSystem(uriData)
                        .mapLoadedData { p -> { Desktop.getDesktop().open(p.toFile()) } }
                }
            }

            else -> flowOf(OptionalLoadable.NotAvailable())
        }
}
