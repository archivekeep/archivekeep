package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable

interface RepositoryItemUiActions : RepositoryBaseUiActions {
    val push: Loadable<Action>
    val pull: Loadable<Action>

    override fun iconActions(): List<Loadable<Action>> = super.iconActions() + listOf(push, pull)
}
