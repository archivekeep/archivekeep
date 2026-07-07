package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference

class HomeNonLocalArchiveUiState(
    val archive: AssociatedArchive,
    val displayName: String,
    val otherRepositories: List<OtherRepositoryDetails>,
) {
    class OtherRepositoryDetails(
        val reference: NamedRepositoryReference,
        val storageReference: StorageNamedReference,
        val repository: Repository,
    )
}
