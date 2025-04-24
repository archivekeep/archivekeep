package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowsDownUp
import compose.icons.tablericons.Lock
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.richcomponents.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitle
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.desktop.ui.designsystem.theme.AppTheme
import org.archivekeep.app.desktop.ui.views.home.HomeArchiveNonLocalArchive
import org.archivekeep.utils.loading.Loadable

@Composable
fun HomeNonLocalArchivesList(otherArchivesLoadable: Loadable<List<HomeArchiveNonLocalArchive>>) {
    LoadableGuard(otherArchivesLoadable) { nonLocalArchives ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingVertical),
        ) {
            if (nonLocalArchives.isEmpty()) {
                Text("Empty")
            }

            nonLocalArchives.forEach { nonLocalArchive ->
                SectionCard {
                    SectionCardTitle(
                        false,
                        nonLocalArchive.displayName,
                        icons = {
                        },
                    )

                    Spacer(Modifier.height(4.dp))

                    SectionCardBottomList(nonLocalArchive.otherRepositories) { repo ->
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
                            if (repo.repository.needsUnlock
                                    .collectAsState(false)
                                    .value
                            ) {
                                Icon(
                                    TablerIcons.Lock,
                                    contentDescription = "Locked",
                                    Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                            }

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f),
                            ) {
                                val name =
                                    repo.storageReference.displayName + (
                                        if (repo.reference.displayName != nonLocalArchive.displayName) {
                                            " (${repo.reference.displayName})"
                                        } else {
                                            ""
                                        }
                                    )

                                Text(
                                    name,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.padding(6.dp)) {
                                    Icon(
                                        TablerIcons.ArrowsDownUp,
                                        contentDescription = "Download",
                                        Modifier.size(16.dp),
                                    )
                                }
                                InArchiveRepositoryDropdownIconLaunched(
                                    repository = repo.repository,
                                    isAssociated = nonLocalArchive.archive.associationId != null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
