package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Upload
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.components.richcomponents.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomListItemIconButton
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardItemStateText
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.desktop.ui.views.home.SecondaryArchiveRepository

@Composable
fun SecondaryArchiveRepositoryRow(
    nonPrimaryRepository: SecondaryArchiveRepository.State,
    name: String = nonPrimaryRepository.repo.reference.displayName,
) {
    val storageRepo = nonPrimaryRepository.repo
    val repository = storageRepo.repository
    val launchers = LocalArchiveOperationLaunchers.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp,
                    horizontal = sectionCardHorizontalPadding,
                ),
    ) {
        if (nonPrimaryRepository.syncRunning) {
            CircularProgressIndicator(
                Modifier
                    .padding(start = 4.dp, end = 12.dp)
                    .then(Modifier.size(14.dp)),
                strokeWidth = 2.dp,
                color = Color.Gray,
            )
        }

        if (nonPrimaryRepository.needsUnlock) {
            Icon(
                TablerIcons.Lock,
                contentDescription = "Locked",
                Modifier
                    .padding(start = 2.dp, end = 8.dp)
                    .then(Modifier.size(16.dp)),
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                name,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 14.sp,
                lineHeight = 16.sp,
            )

            nonPrimaryRepository.texts.let { SectionCardItemStateText(it) }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionCardBottomListItemIconButton(
                TablerIcons.Upload,
                contentDescription = "Push",
                enabled = nonPrimaryRepository.canPush,
                onClick = {
                    launchers.pushToRepo(
                        storageRepo.reference.uri,
                        storageRepo.primaryRepositoryURI!!,
                    )
                },
            )
            SectionCardBottomListItemIconButton(
                TablerIcons.Download,
                contentDescription = "Pull",
                enabled = nonPrimaryRepository.canPull,
                onClick = {
                    launchers.pullFromRepo(
                        storageRepo.reference.uri,
                        storageRepo.primaryRepositoryURI!!,
                    )
                },
            )
            InArchiveRepositoryDropdownIconLaunched(
                repository = repository,
                isAssociated = storageRepo.otherRepositoryState.associationId != null,
            )
        }
    }
}
