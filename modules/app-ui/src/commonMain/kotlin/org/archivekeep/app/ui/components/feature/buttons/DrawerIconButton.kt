package org.archivekeep.app.ui.components.feature.buttons

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

fun drawerIconButton(drawerState: DrawerState?): @Composable (() -> Unit)? =
    drawerState?.let { drawerState ->
        {
            val scope = rememberCoroutineScope()

            IconButton(onClick = {
                scope.launch {
                    if (drawerState.isClosed) {
                        drawerState.open()
                    } else {
                        drawerState.close()
                    }
                }
            }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White.copy(alpha = LocalContentAlpha.current),
                )
            }
        }
    }
