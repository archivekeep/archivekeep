package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowScope
import compose.icons.TablerIcons
import compose.icons.tablericons.Database
import compose.icons.tablericons.Folders
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRail
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRailItem
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors.Companion.appBarBackground
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.archives.ArchivesView
import org.archivekeep.app.desktop.ui.views.home.HomeView
import org.archivekeep.app.desktop.ui.views.settings.SettingsView
import org.archivekeep.app.desktop.ui.views.storages.StoragesView

@Composable
fun WindowScope.MainWindowLayout(onCloseRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<NavigableView<View<*>>>(navigables[0]) }

    @Composable
    fun <V> renderView(
        view: View<V>,
        current: Boolean,
        modifier: Modifier,
    ) {
        val state = view.producePersistentState(scope)

        if (current) {
            view.render(modifier, state)
        }
    }

    Column {
        AppBar(onCloseRequest = onCloseRequest)
        Row {
            WindowDraggableArea {
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

@Composable
private fun WindowScope.AppBar(onCloseRequest: () -> Unit) {
    WindowDraggableArea {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("ArchiveKeep", modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                        Text("personal files archivation", fontSize = 12.sp)
                    }
                },
                backgroundColor = appBarBackground,
                actions = {
                    IconButton(onClick = onCloseRequest, content = {
                        Icon(imageVector = Icons.Default.Close, "Close application")
                    })
                    Spacer(Modifier.width(12.dp))
                },
            )
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
            key = "settings",
            title = "Settings",
            icon = Icons.Default.Settings,
            view = SettingsView(),
        ),
    )

private val allNavigables = navigables + navigablesEnd
