package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.appbar_unlock_credentials
import org.archivekeep.ui.resources.common_locked
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppBarCredentialsUnlockButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
    ) {
        Icon(
            TablerIcons.Lock,
            tint = Color(233, 66, 66),
            contentDescription = stringResource(Res.string.common_locked),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))

        Text(stringResource(Res.string.appbar_unlock_credentials))
    }
}
