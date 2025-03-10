package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.views.home.HomeViewAction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeActionsList(allActions: List<HomeViewAction>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        allActions.forEach { action ->
            OutlinedButton(onClick = action.onTrigger) {
                Text(action.title)
            }
        }
    }
}
