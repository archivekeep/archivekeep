package org.archivekeep.app.desktop.ui.views.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.ui.components.demo.PresetSettings
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.views.View

class SettingsView : View<String> {
    @Composable
    override fun producePersistentState(scope: CoroutineScope): String = "TODO"

    @Composable
    override fun render(
        modifier: Modifier,
        state: String,
    ) {
        Surface(
            modifier = modifier,
            color = CColors.cardsGridBackground,
        ) {
            Column(
                modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                PresetSettings()
            }
        }
    }
}
