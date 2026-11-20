package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.ui.views.home.components.OnboardingPage
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

sealed class AssociationBannerState {
    data object NoAssociation : AssociationBannerState()

    data object RemoteOnlyAssociated : AssociationBannerState()

    data object BothSeparate : AssociationBannerState()
}

class HomeViewState(
    val localArchives: Loadable<List<HomeLocalArchiveModel>>,
    val nonLocalArchives: Loadable<List<HomeExternalArchiveModel>>,
    val externalStorages: Loadable<HomeStorageListUiState>,
) {
    val showBaseLoading = localArchives.isLoading || externalStorages.isLoading

    val showExternalStoragesSection = externalStorages.mapIfLoadedOrDefault(false) { it.availableStorages.isNotEmpty() }

    // Determine which association banner state to show
    val associationBannerState: AssociationBannerState?
        get() {
            val localList = (localArchives as? Loadable.Loaded)?.value ?: return null
            val nonLocalList = (nonLocalArchives as? Loadable.Loaded)?.value ?: return null

            val hasLocal = localList.isNotEmpty()
            val hasNonLocal = nonLocalList.isNotEmpty()

            // Banner is not shown when there's already an archive spanning local and remote
            val hasSpanningArchive = localList.any { it.otherRepositories.isNotEmpty() }
            if (hasSpanningArchive) return null

            // Banner requires at least one local archive
            if (!hasLocal) return null

            val anyLocalAssociated = localList.any { it.isAssociated }
            val anyNonLocalAssociated = nonLocalList.any { it.isAssociated }

            return when {
                hasNonLocal && anyLocalAssociated && anyNonLocalAssociated -> AssociationBannerState.BothSeparate
                hasNonLocal && !anyLocalAssociated && anyNonLocalAssociated -> AssociationBannerState.RemoteOnlyAssociated
                else -> AssociationBannerState.NoAssociation
            }
        }

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
