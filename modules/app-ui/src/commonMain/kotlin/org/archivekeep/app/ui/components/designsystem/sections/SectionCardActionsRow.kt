package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.base.layout.OverflowRow
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_loading_dots
import org.archivekeep.ui.resources.common_more_actions
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading
import org.jetbrains.compose.resources.stringResource

@Composable
fun SectionCardActionsRow(
    actions: List<Loadable<Action>>,
    noActionsText: String,
) {
    val pendingActions = actions.filterIsInstance<Loadable.Loaded<Action>>().filter { it.value.isPending && it.value.isAvailable }.map { it.value }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 4.dp,
                    start = sectionCardHorizontalPadding,
                    bottom = 12.dp,
                    end = sectionCardHorizontalPadding,
                ),
    ) {
        if (pendingActions.isEmpty()) {
            if (actions.none { it.isLoading }) {
                SectionCardActionsRowText(noActionsText, modifier = Modifier.fillMaxWidth())
            } else {
                SectionCardActionsRowText(stringResource(Res.string.common_loading_dots), modifier = Modifier.fillMaxWidth())
            }
        } else {
            OverflowRow(
                items = pendingActions,
                spacing = 6.dp,
                itemContent = { action ->
                    SectionCardButton(
                        onClick = action.onLaunch,
                        text = action.title,
                        running = action.running,
                    )
                },
                overflowContent = { hiddenActions ->
                    var isDropdownExpanded by remember { mutableStateOf(false) }
                    val moreActionsLabel = stringResource(Res.string.common_more_actions)

                    Box {
                        IconButton(
                            onClick = { isDropdownExpanded = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreHoriz,
                                contentDescription = moreActionsLabel,
                            )
                        }

                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                        ) {
                            hiddenActions.forEach { action ->
                                DropdownMenuItem(
                                    onClick = {
                                        action.onLaunch()
                                        isDropdownExpanded = false
                                    },
                                    text = { Text(action.title) },
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
