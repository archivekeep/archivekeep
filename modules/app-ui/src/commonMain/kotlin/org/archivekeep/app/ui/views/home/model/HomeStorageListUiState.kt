package org.archivekeep.app.ui.views.home.model

data class HomeStorageListUiState(
    val isLoadingSomeItems: Boolean,
    val hasAnyRegistered: Boolean,
    val availableStorages: List<HomeStorageUiState>,
)
