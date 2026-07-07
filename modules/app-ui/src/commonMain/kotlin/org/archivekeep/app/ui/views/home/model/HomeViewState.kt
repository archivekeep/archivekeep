package org.archivekeep.app.ui.views.home.model

import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

class HomeViewState(
    val localArchives: Loadable<List<HomeLocalArchiveModel>>,
    val nonLocalArchives: Loadable<List<HomeNonLocalArchiveUiState>>,
    val externalStorages: Loadable<HomeStorageListUiState>,
) {
    val showBaseLoading = localArchives.isLoading || externalStorages.isLoading

    val showLocalAddIntro = if (localArchives is Loadable.Loaded) localArchives.value.isEmpty() else false
    val showExternalAddIntro = if (externalStorages is Loadable.Loaded) !externalStorages.value.hasAnyRegistered else false

    val showExternalStoragesSection = externalStorages.mapIfLoadedOrDefault(false) { it.availableStorages.isNotEmpty() }
}
