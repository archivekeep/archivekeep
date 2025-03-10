package org.archivekeep.app.desktop.ui.views.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.home.components.HomeArchiveKeepFunctionDescription

class InfoView : View<Unit> {
    @Composable
    override fun producePersistentState(scope: CoroutineScope) {
    }

    @Composable
    override fun render(
        modifier: Modifier,
        state: Unit,
    ) {
        Surface(
            modifier,
            color = CColors.cardsGridBackground,
        ) {
            homeViewContent(state)
        }
    }
}

@Composable
private fun homeViewContent(vm: Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            HomeArchiveKeepFunctionDescription()
        }
    }
}
