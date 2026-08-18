package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable

@Composable
fun SectionCardBottomListItemIconAvailableActions(actions: List<Loadable<Action>>) {
    actions
        .filterIsInstance<Loadable.Loaded<Action>>()
        .map { it.value }
        .filter { it.isAvailable }
        .forEach { SectionCardBottomListItemIconActionButton(it) }
}
