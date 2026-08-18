package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable

interface HomeLocalArchiveUiActions : RepositoryBaseUiActions {
    val addPush: Loadable<Action>
    val push: Loadable<Action>

    fun asList(): List<Loadable<Action>> =
        listOfNotNull(
            unlock,
            associate,
            unassociate,
            reindex,
            cleanupFiles,
            addPush,
            add,
            push,
        )
}
