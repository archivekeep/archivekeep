package org.archivekeep.app.desktop.ui.views.home

import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

class HomeViewState(
    val allLocalArchivesLoadable: Loadable<List<HomeArchiveEntryViewModel>>,
    val otherArchives: Loadable<List<HomeArchiveNonLocalArchive>>,
    val externalStoragesLoadable: Loadable<HomeStoragesState>,
) {
    val showBaseLoading = allLocalArchivesLoadable.isLoading || externalStoragesLoadable.isLoading

    val showLocalAddIntro = if (allLocalArchivesLoadable is Loadable.Loaded) allLocalArchivesLoadable.value.isEmpty() else false
    val showExternalAddIntro = if (externalStoragesLoadable is Loadable.Loaded) !externalStoragesLoadable.value.hasAnyRegistered else false

    val showExternalStoragesSection = externalStoragesLoadable.mapIfLoadedOrDefault(false) { it.availableStorages.isNotEmpty() }
}

data class HomeStoragesState(
    val isLoadingSomeItems: Boolean,
    val hasAnyRegistered: Boolean,
    val availableStorages: List<HomeViewStorage>,
)
