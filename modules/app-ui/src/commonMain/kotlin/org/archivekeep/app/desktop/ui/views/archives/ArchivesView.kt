package org.archivekeep.app.desktop.ui.views.archives

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.views.View

class ArchivesView : View<String> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope): String = "TODO"

    @Composable
    override fun render(
        modifier: Modifier,
        vm: String,
    ) {
        Surface(
            modifier = modifier,
            color = CColors.cardsGridBackground,
        ) {
            Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text("To be implemented ...")
            }
        }
    }
}
