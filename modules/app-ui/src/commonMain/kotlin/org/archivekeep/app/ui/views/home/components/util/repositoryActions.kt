package org.archivekeep.app.ui.views.home.components.util

import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.views.home.model.RepositoryBaseUiActions
import org.archivekeep.app.ui.views.home.model.RepositoryItemUiActions
import org.archivekeep.utils.loading.Loadable

fun RepositoryBaseUiActions.baseActions(): List<Loadable<Action>> =
    listOf(
        unlock,
        add,
        cleanupFiles,
        reindex,
    )

fun RepositoryItemUiActions.itemActions(): List<Loadable<Action>> = baseActions() + listOf(push, pull)
