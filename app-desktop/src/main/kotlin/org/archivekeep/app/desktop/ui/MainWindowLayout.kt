package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.Database
import compose.icons.tablericons.Folders
import compose.icons.tablericons.InfoSquare
import kotlinx.coroutines.plus
import org.archivekeep.app.desktop.domain.services.SharingCoroutineDispatcher
import org.archivekeep.app.desktop.ui.components.AppBar
import org.archivekeep.app.desktop.ui.components.DraggableAreaIfWindowPresent
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRail
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRailItem
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.archives.ArchivesView
import org.archivekeep.app.desktop.ui.views.home.HomeView
import org.archivekeep.app.desktop.ui.views.home.InfoView
import org.archivekeep.app.desktop.ui.views.settings.SettingsView
import org.archivekeep.app.desktop.ui.views.storages.StoragesView

@Composable
fun MainWindowLayout(onCloseRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    val sharingScope =
        with(SharingCoroutineDispatcher) {
            remember(scope) {
                scope + this
            }
        }

    var selectedItem by remember { mutableStateOf<NavigableView<View<*>>>(navigables[0]) }

    @Composable
    fun <V> renderView(
        view: View<V>,
        current: Boolean,
        modifier: Modifier,
    ) {
        val state = view.producePersistentState(sharingScope)

        if (current) {
            view.render(modifier, state)
        }
    }

    Column {
        AppBar(onCloseRequest = onCloseRequest)
        Row {
            DraggableAreaIfWindowPresent {
                NavigationRail {
                    @Composable
                    fun renderItem(it: NavigableView<View<*>>) {
                        NavigationRailItem(
                            text = it.title,
                            icon = it.icon,
                            selected = selectedItem == it,
                            onClick = { selectedItem = it },
                        )
                    }

                    navigables.forEach {
                        renderItem(it)
                    }
                    Spacer(Modifier.weight(1f))
                    navigablesEnd.forEach {
                        renderItem(it)
                    }
                }
            }

            allNavigables.forEach {
                renderView(
                    it.view,
                    selectedItem == it,
                    Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

data class NavigableView<out V : View<*>>(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val view: V,
)

private val navigables =
    listOf(
        NavigableView(
            key = "home",
            title = "Home",
            icon = Icons.Default.Home,
            view = HomeView(),
        ),
        NavigableView(
            key = "archives",
            title = "Archives",
            icon = TablerIcons.Folders,
            view = ArchivesView(),
        ),
        NavigableView(
            key = "storages",
            title = "Storages",
            icon = TablerIcons.Database,
            view = StoragesView(),
        ),
    )

private val navigablesEnd =
    listOf(
        NavigableView(
            key = "info",
            title = "Info",
            icon = TablerIcons.InfoSquare,
            view = InfoView(),
        ),
        NavigableView(
            key = "settings",
            title = "Settings",
            icon = Icons.Default.Settings,
            view = SettingsView(),
        ),
    )

private val allNavigables = navigables + navigablesEnd
