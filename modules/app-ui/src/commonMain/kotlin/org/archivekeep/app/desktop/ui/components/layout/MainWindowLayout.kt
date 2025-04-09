package org.archivekeep.app.desktop.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import org.archivekeep.app.desktop.ui.components.DraggableAreaIfWindowPresent
import org.archivekeep.app.desktop.ui.components.VersionText
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationBar
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRail
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationRailBarItem
import org.archivekeep.app.desktop.ui.designsystem.navigation.NavigationTopBarItem
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors.Companion.navigationRailColor
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.archives.ArchivesView
import org.archivekeep.app.desktop.ui.views.home.HomeView
import org.archivekeep.app.desktop.ui.views.home.InfoView
import org.archivekeep.app.desktop.ui.views.settings.SettingsView
import org.archivekeep.app.desktop.ui.views.storages.StoragesView

@Composable
fun MainWindowLayout(
    applicationNavigationLayout: ApplicationNavigationLayout,
    onCloseRequest: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()

    val sharingScope =
        with(SharingCoroutineDispatcher) {
            remember(scope) {
                scope + this
            }
        }

    val selectedItem = remember { mutableStateOf<NavigableView<View<*>>>(navigables[0]) }

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

    OptionalModalNavigationDrawer(
        enabled = applicationNavigationLayout == ApplicationNavigationLayout.TOP_BAR_AND_DRAWER,
        navigationItems = { close ->
            AppDrawerNavigation(Modifier.weight(1f), selectedItem, close)
        },
    ) { drawerState ->
        Column {
            if (applicationNavigationLayout != ApplicationNavigationLayout.RAIL_BAR) {
                AppBar(drawerState = drawerState, onCloseRequest = onCloseRequest) {
                    if (applicationNavigationLayout == ApplicationNavigationLayout.TOP_BAR_EMBEDDED_NAVIGATION) {
                        AppBarNavigation(selectedItem)
                    }
                }
            }

            if (applicationNavigationLayout == ApplicationNavigationLayout.RAIL_BAR) {
                Spacer(
                    Modifier
                        .windowInsetsTopHeight(WindowInsets.safeDrawing)
                        .background(CColors.appBarBackground)
                        .fillMaxWidth(),
                )
            }

            val insetsSides =
                when (applicationNavigationLayout) {
                    ApplicationNavigationLayout.RAIL_BAR ->
                        WindowInsetsSides.Bottom + WindowInsetsSides.End
                    ApplicationNavigationLayout.TOP_AND_BOTTOM ->
                        WindowInsetsSides.Horizontal
                    else ->
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                }

            Row(
                Modifier
                    .weight(1f, fill = true)
                    .background(navigationRailColor)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(insetsSides)),
            ) {
                if (applicationNavigationLayout == ApplicationNavigationLayout.RAIL_BAR) {
                    RailBar(selectedItem)
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    allNavigables.forEach {
                        renderView(
                            it.view,
                            selectedItem.value == it,
                            Modifier.fillMaxWidth().fillMaxHeight(),
                        )
                    }
                }
            }

            if (applicationNavigationLayout == ApplicationNavigationLayout.TOP_AND_BOTTOM) {
                BottomBar(selectedItem)
            }
        }
    }
}

@Composable
private fun AppDrawerNavigation(
    modifier: Modifier,
    selectedItem: MutableState<NavigableView<View<*>>>,
    close: () -> Unit,
) {
    @Composable
    fun renderNavigationDrawerItem(
        it: NavigableView<View<*>>,
        onSelect: () -> Unit,
    ) {
        NavigationDrawerItem(
            label = { Text(it.title) },
            icon = { Icon(it.icon, contentDescription = null) },
            selected = selectedItem.value == it,
            onClick = {
                selectedItem.value = it
                onSelect()
            },
        )
    }

    Column(
        modifier =
            modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        navigables.forEach {
            renderNavigationDrawerItem(it, onSelect = { close() })
        }
        Spacer(Modifier.weight(1f))
        VersionText()
        navigablesEnd.forEach {
            renderNavigationDrawerItem(it, onSelect = { close() })
        }
    }
}

@Composable
private fun AppBarNavigation(selectedItem: MutableState<NavigableView<View<*>>>) {
    @Composable
    fun renderTopBarItem(
        modifier: Modifier,
        it: NavigableView<View<*>>,
    ) {
        NavigationTopBarItem(
            text = it.title,
            icon = it.icon,
            selected = selectedItem.value == it,
            onClick = { selectedItem.value = it },
            modifier = modifier,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (navigables + navigablesEnd).forEach {
            renderTopBarItem(Modifier.defaultMinSize(minWidth = 60.dp), it)
        }
    }
}

@Composable
fun renderItem(
    modifier: Modifier,
    selectedItem: MutableState<NavigableView<View<*>>>,
    it: NavigableView<View<*>>,
) {
    NavigationRailBarItem(
        text = it.title,
        icon = it.icon,
        selected = selectedItem.value == it,
        onClick = { selectedItem.value = it },
        modifier = modifier,
    )
}

@Composable
private fun RailBar(selectedItem: MutableState<NavigableView<View<*>>>) {
    DraggableAreaIfWindowPresent {
        NavigationRail {
            navigables.forEach {
                renderItem(Modifier.fillMaxWidth(), selectedItem, it)
            }
            Spacer(Modifier.weight(1f))
            VersionText()
            navigablesEnd.forEach {
                renderItem(Modifier.fillMaxWidth(), selectedItem, it)
            }
        }
    }
}

@Composable
private fun BottomBar(selectedItem: MutableState<NavigableView<View<*>>>) {
    DraggableAreaIfWindowPresent {
        NavigationBar {
            (navigables + navigablesEnd).forEach {
                renderItem(Modifier.fillMaxHeight().defaultMinSize(minWidth = 40.dp), selectedItem, it)
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
