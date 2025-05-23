package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeArchivesIntro() {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    SectionCard(
        Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                "1. setup local storage",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Begin by adding an local repository to contain data you want to have archived.",
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
            Text(
                "It can already contain existing data.",
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        archiveOperationLaunchers.openAddFileSystemRepository(FileSystemStorageType.LOCAL)
                    },
                    modifier = Modifier.defaultMinSize(120.dp),
                ) {
                    Text("Add local repository…")
                }
            }
        }
    }
}
