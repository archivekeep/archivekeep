package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.loading.optional.OptionalLoadable

data class HomeExternalArchiveRepositoryUiState(
    val uri: RepositoryURI,
    val name: String,
    val statusText: OptionalLoadable.LoadedAvailable<String>,
    val isLoading: Boolean,
    val needsUnlock: Boolean,
    val repositoryOperationalState: RepositoryOperationalState,
)
