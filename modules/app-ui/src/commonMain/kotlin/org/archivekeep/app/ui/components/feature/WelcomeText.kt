package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeText() {
    Column {
        Text(
            "Welcome to ArchiveKeep",
            modifier = Modifier.padding(top = 20.dp, bottom = 0.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Personal archivation - file synchronization and replication across multiple storages (repositories)",
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
