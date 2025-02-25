package org.archivekeep.app.core.domain.archives

import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.utils.loading.Loadable

interface ArchiveService {
    val allArchives: SharedFlow<Loadable<List<AssociatedArchive>>>
}
