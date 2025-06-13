package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.persistence.credentials.PasswordProtectedDataStore
import org.archivekeep.app.ui.components.base.interactivity.DraggableAreaIfWindowPresent
import org.archivekeep.app.ui.components.designsystem.navigation.AppBarIconButton
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.app.ui.components.feature.buttons.drawerIconButton
import org.archivekeep.app.ui.domain.data.canUnlock
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers

@Composable
fun AppBar(
    drawerState: DrawerState?,
    onCloseRequest: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val credentialStorage = LocalApplicationServices.current.environment.walletDataStore as? PasswordProtectedDataStore

    val canUnlock = credentialStorage.canUnlock()

    DraggableAreaIfWindowPresent {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
        ) {
            TopAppBar(
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                navigationIcon = drawerIconButton(drawerState),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ArchiveKeep", modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                        Spacer(Modifier.width(20.dp))
                        content()
                    }
                },
                backgroundColor = CColors.appBarBackground,
                actions = {
                    Row(
                        Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canUnlock) {
                            AppBarCredentialsUnlockButton(
                                onClick = LocalWalletOperationLaunchers.current.let { { it.openUnlockWallet(null) } },
                            )
                        }

                        MainMenuDropdownIconLaunched()

                        onCloseRequest?.let {
                            AppBarIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close application",
                                onClick = it,
                            )
                        }
                    }
                },
            )
        }
    }
}
