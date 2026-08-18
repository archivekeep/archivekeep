package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable

interface RepositoryBaseUiActions {
    val open: Loadable<Action>
    val unlock: Loadable<Action>
    val add: Loadable<Action>
    val cleanupFiles: Loadable<Action>
    val reindex: Loadable<Action>
    val associate: Loadable<Action>
    val unassociate: Loadable<Action>
    val forget: Loadable<Action>
    val deinitialize: Loadable<Action>

    fun iconActions(): List<Loadable<Action>> =
        listOf(
            unlock,
            add,
            cleanupFiles,
            reindex,
        )
}
