package org.archivekeep.app.desktop.ui.views.home

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.ui.views.home.components.HomeArchiveKeepFunctionDescription
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.app.ui.components.feature.WelcomeText
import org.archivekeep.app.ui.views.View

class InfoView : View<Unit> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope) {
    }

    @Composable
    override fun render(
        modifier: Modifier,
        vm: Unit,
    ) {
        Surface(
            modifier,
            color = CColors.cardsGridBackground,
        ) {
            homeViewContent(vm)
        }
    }
}

@Composable
private fun homeViewContent(vm: Unit) {
    ViewScrollableContainer {
        WelcomeText()

        HomeArchiveKeepFunctionDescription()
    }
}
