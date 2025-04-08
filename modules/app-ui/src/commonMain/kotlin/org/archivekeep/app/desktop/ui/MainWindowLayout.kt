package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Database
import compose.icons.tablericons.Folders
import compose.icons.tablericons.InfoSquare
import kotlinx.coroutines.plus
import org.archivekeep.app.desktop.domain.services.SharingCoroutineDispatcher
import org.archivekeep.app.desktop.enableUnfinishedFeatures
import org.archivekeep.app.desktop.ui.components.AppBar
import org.archivekeep.app.desktop.ui.components.DraggableAreaIfWindowPresent
import org.archivekeep.app.desktop.ui.components.VersionText
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationBar
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRail
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRailBarItem
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.archives.ArchivesView
import org.archivekeep.app.desktop.ui.views.home.HomeView
import org.archivekeep.app.desktop.ui.views.home.InfoView
import org.archivekeep.app.desktop.ui.views.settings.SettingsView
import org.archivekeep.app.desktop.ui.views.storages.StoragesView

@Composable
fun MainWindowLayout(
    windowSizeClass: WindowSizeClass,
    onCloseRequest: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()

    val sharingScope =
        with(SharingCoroutineDispatcher) {
            remember(scope) {
                scope + this
            }
        }

    var selectedItem by remember { mutableStateOf<NavigableView<View<*>>>(navigables[0]) }

    val showRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

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

    @Composable
    fun renderItem(
        modifier: Modifier,
        it: NavigableView<View<*>>,
    ) {
        NavigationRailBarItem(
            text = it.title,
            icon = it.icon,
            selected = selectedItem == it,
            onClick = { selectedItem = it },
            modifier = modifier,
        )
    }

    Column {
        AppBar(onCloseRequest = onCloseRequest)
        Row(Modifier.weight(1f, fill = true)) {
            if (showRail) {
                DraggableAreaIfWindowPresent {
                    NavigationRail {
                        navigables.forEach {
                            renderItem(Modifier.fillMaxWidth(), it)
                        }
                        Spacer(Modifier.weight(1f))
                        VersionText()
                        navigablesEnd.forEach {
                            renderItem(Modifier.fillMaxWidth(), it)
                        }
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

        if (!showRail) {
            DraggableAreaIfWindowPresent {
                NavigationBar {
                    (navigables + navigablesEnd).forEach {
                        renderItem(Modifier.fillMaxHeight().defaultMinSize(minWidth = 40.dp), it)
                    }
                }
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
    listOfNotNull(
        NavigableView(
            key = "home",
            title = "Home",
            icon = Icons.Default.Home,
            view = HomeView(),
        ),
        if (enableUnfinishedFeatures) {
            NavigableView(
                key = "archives",
                title = "Archives",
                icon = TablerIcons.Folders,
                view = ArchivesView(),
            )
        } else {
            null
        },
        NavigableView(
            key = "storages",
            title = "Storages",
            icon = TablerIcons.Database,
            view = StoragesView(),
        ),
    )

private val navigablesEnd =
    listOfNotNull(
        NavigableView(
            key = "info",
            title = "Info",
            icon = TablerIcons.InfoSquare,
            view = InfoView(),
        ),
        if (enableUnfinishedFeatures) {
            NavigableView(
                key = "settings",
                title = "Settings",
                icon = Icons.Default.Settings,
                view = SettingsView(),
            )
        } else {
            null
        },
    )

private val allNavigables = navigables + navigablesEnd
