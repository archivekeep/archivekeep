package org.archivekeep.app.ui.views.home.components

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewLoading
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.ui.components.designsystem.sections.SectionBlock
import org.archivekeep.app.ui.views.home.model.HomeViewState
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_section_external_archives
import org.archivekeep.ui.resources.home_section_external_storages
import org.archivekeep.ui.resources.home_section_local_archives
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapLoadedData
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeViewContent(state: HomeViewState) {
    if (state.showBaseLoading) {
        ViewLoading()
        return
    }

    if (state.showOnboardingCarousel) {
        OnboardingCarousel(currentPage = state.onboardingStartPage)
        return
    }

    ViewScrollableContainer {
        SectionBlock(stringResource(Res.string.home_section_local_archives)) {
            HomeArchivesList(state.localArchives)
        }

        state.nonLocalArchives.let { otherArchives ->
            if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                SectionBlock(stringResource(Res.string.home_section_external_archives)) {
                    HomeNonLocalArchivesList(otherArchives)
                }
            }
        }

        if (state.showExternalStoragesSection) {
            SectionBlock(
                stringResource(Res.string.home_section_external_storages),
                isLoading = state.externalStorages.mapIfLoadedOrDefault(true) { it.isLoadingSomeItems },
            ) {
                HomeStoragesList(state.externalStorages.mapLoadedData { it.availableStorages })
            }
        }
    }
}
