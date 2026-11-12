package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewLoading
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.ui.components.designsystem.sections.SectionBlock
import org.archivekeep.app.ui.components.feature.WelcomeText
import org.archivekeep.app.ui.views.home.model.HomeViewState
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_section_external_archives
import org.archivekeep.ui.resources.home_section_external_storages
import org.archivekeep.ui.resources.home_section_introduction
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

    ViewScrollableContainer {
        if (state.showLocalAddIntro) {
            WelcomeText()

            SectionBlock(stringResource(Res.string.home_section_introduction)) {
                HomeArchivesIntro()
                if (state.showExternalAddIntro) {
                    Spacer(Modifier.height(16.dp))
                    HomeStoragesIntro()
                }
            }
        } else {
            SectionBlock(stringResource(Res.string.home_section_local_archives)) {
                HomeArchivesList(state.localArchives)
            }
        }

        state.nonLocalArchives.let { otherArchives ->
            if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                SectionBlock(stringResource(Res.string.home_section_external_archives)) {
                    HomeNonLocalArchivesList(otherArchives)
                }
            }
        }

        if (state.showExternalAddIntro) {
            if (!state.showLocalAddIntro) {
                SectionBlock(stringResource(Res.string.home_section_introduction)) {
                    HomeStoragesIntro()
                }
            }
        } else {
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
}
