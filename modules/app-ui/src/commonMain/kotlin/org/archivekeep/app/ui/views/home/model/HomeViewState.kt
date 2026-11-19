package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.ui.views.home.components.OnboardingPage
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

class HomeViewState(
    val localArchives: Loadable<List<HomeLocalArchiveModel>>,
    val nonLocalArchives: Loadable<List<HomeExternalArchiveModel>>,
    val externalStorages: Loadable<HomeStorageListUiState>,
) {
    val showBaseLoading = localArchives.isLoading || externalStorages.isLoading

    val showLocalAddIntro = if (localArchives is Loadable.Loaded) localArchives.value.isEmpty() else false
    val showExternalAddIntro = if (externalStorages is Loadable.Loaded) !externalStorages.value.hasAnyRegistered else false

    val showExternalStoragesSection = externalStorages.mapIfLoadedOrDefault(false) { it.availableStorages.isNotEmpty() }

    // Onboarding carousel visibility and initial page
    val showOnboardingCarousel: Boolean
    val onboardingStartPage: OnboardingPage

    init {
        val hasLocal = localArchives is Loadable.Loaded && localArchives.value.isNotEmpty()
        val hasExternal = externalStorages is Loadable.Loaded && externalStorages.value.hasAnyRegistered

        when {
            !hasLocal && !hasExternal -> {
                // Nothing added yet — show carousel starting at intro
                showOnboardingCarousel = true
                onboardingStartPage = OnboardingPage.Intro
            }
            hasLocal && !hasExternal -> {
                // Local repo added but no external — show carousel starting at remote page
                showOnboardingCarousel = true
                onboardingStartPage = OnboardingPage.RemoteStorage
            }
            else -> {
                // Both local and external added — no onboarding
                showOnboardingCarousel = false
                onboardingStartPage = OnboardingPage.Intro
            }
        }
    }
}
