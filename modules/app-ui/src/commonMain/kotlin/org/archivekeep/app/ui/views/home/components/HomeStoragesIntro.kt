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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_storages_intro_add_directory_button
import org.archivekeep.ui.resources.home_storages_intro_add_url_button
import org.archivekeep.ui.resources.home_storages_intro_description
import org.archivekeep.ui.resources.home_storages_intro_description_details
import org.archivekeep.ui.resources.home_storages_intro_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeStoragesIntro() {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    SectionCard(
        Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                stringResource(Res.string.home_storages_intro_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                stringResource(Res.string.home_storages_intro_description),
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
            Text(
                stringResource(Res.string.home_storages_intro_description_details),
                color = Color.DarkGray,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        archiveOperationLaunchers.openAddFileSystemRepository()
                    },
                    modifier = Modifier.defaultMinSize(120.dp),
                ) {
                    Text(stringResource(Res.string.home_storages_intro_add_directory_button))
                }
                TextButton(
                    onClick = {
                        archiveOperationLaunchers.openAddRemoteRepository()
                    },
                    modifier = Modifier.defaultMinSize(120.dp),
                ) {
                    Text(stringResource(Res.string.home_storages_intro_add_url_button))
                }
            }
        }
    }
}
