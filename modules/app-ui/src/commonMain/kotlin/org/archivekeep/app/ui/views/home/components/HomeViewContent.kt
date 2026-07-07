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
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapLoadedData

@Composable
fun HomeViewContent(state: HomeViewState) {
    if (state.showBaseLoading) {
        ViewLoading()
        return
    }

    ViewScrollableContainer {
        if (state.showLocalAddIntro) {
            WelcomeText()

            SectionBlock("Introduction") {
                HomeArchivesIntro()
                if (state.showExternalAddIntro) {
                    Spacer(Modifier.height(16.dp))
                    HomeStoragesIntro()
                }
            }
        } else {
            SectionBlock("Local archives") {
                HomeArchivesList(state.localArchives)
            }
        }

        state.nonLocalArchives.let { otherArchives ->
            if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                SectionBlock("External archives") {
                    HomeNonLocalArchivesList(otherArchives)
                }
            }
        }

        if (state.showExternalAddIntro) {
            if (!state.showLocalAddIntro) {
                SectionBlock("Introduction") {
                    HomeStoragesIntro()
                }
            }
        } else {
            if (state.showExternalStoragesSection) {
                SectionBlock(
                    "External storages",
                    isLoading = state.externalStorages.mapIfLoadedOrDefault(true) { it.isLoadingSomeItems },
                ) {
                    HomeStoragesList(state.externalStorages.mapLoadedData { it.availableStorages })
                }
            }
        }
    }
}
