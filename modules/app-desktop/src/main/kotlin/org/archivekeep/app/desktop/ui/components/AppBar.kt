package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.desktop.domain.data.canUnlock
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.ui.designsystem.appbar.AppBarCredentialsUnlockButton
import org.archivekeep.app.desktop.ui.designsystem.appbar.AppBarIconButton
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors

@Composable
fun AppBar(onCloseRequest: () -> Unit) {
    val credentialStorage = LocalWalletDataStore.current

    val canUnlock = credentialStorage.canUnlock()

    DraggableAreaIfWindowPresent {
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

                        AppBarIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Close application",
                            onClick = onCloseRequest,
                        )
                    }
                },
            )
        }
    }
}
