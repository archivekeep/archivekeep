package org.archivekeep.app.core.domain.archives

import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.utils.Loadable

interface ArchiveService {
    val allArchives: SharedFlow<Loadable<List<AssociatedArchive>>>
}
