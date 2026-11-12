package org.archivekeep.app.ui.components.designsystem.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_disconnected
import org.archivekeep.ui.resources.common_online
import org.archivekeep.ui.resources.connection_connected
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectionStatusTag(connectionStatus: Storage.ConnectionStatus) {
    val (color, text) =
        when (connectionStatus) {
            Storage.ConnectionStatus.ONLINE -> Pair(Color(190, 235, 225), stringResource(Res.string.common_online))
            Storage.ConnectionStatus.CONNECTED -> Pair(Color(244, 211, 160), stringResource(Res.string.connection_connected))
            Storage.ConnectionStatus.DISCONNECTED -> Pair(Color(221, 232, 221), stringResource(Res.string.common_disconnected))
        }

    Surface(
        color = color,
        border = BorderStroke(1.dp, Color(0, 0, 0, 11)),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
            color = Color(0, 0, 0, 150),
            fontSize = 11.sp,
            lineHeight = 11.sp,
        )
    }
}
