package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading

@Composable
fun SectionCardActionsRow(
    actions: List<Loadable<Action>>,
    noActionsText: String,
) {
    val availableActions = actions.filterIsInstance<Loadable.Loaded<Action>>().filter { it.value.isAvailable }.map { it.value }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        if (availableActions.isEmpty()) {
            if (actions.none { it.isLoading }) {
                SectionCardActionsRowText(noActionsText, modifier = Modifier.fillMaxWidth())
            } else {
                SectionCardActionsRowText("Loading.", modifier = Modifier.fillMaxWidth())
            }
        } else {
            val firstAction = availableActions.first()
            val remainingActions = availableActions.subList(1, availableActions.size)

            SectionCardButton(
                onClick = firstAction.onLaunch,
                text = firstAction.text,
                running = firstAction.running,
            )

            if (remainingActions.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    var isDropdownExpanded by remember { mutableStateOf(false) }

                    SectionCardSecondaryButton(
                        onClick = {
                            isDropdownExpanded = true
                        },
                        text = "More",
                        modifier =
                            Modifier.semantics {
                                contentDescription = "More actions"
                            },
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        remainingActions.forEach { action ->
                            DropdownMenuItem(
                                onClick = {
                                    action.onLaunch()
                                    isDropdownExpanded = false
                                },
                                text = {
                                    Text(action.text)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
