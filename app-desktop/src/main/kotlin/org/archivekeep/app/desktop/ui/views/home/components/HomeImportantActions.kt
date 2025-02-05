package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers

@Composable
fun HomeImportantActions() {
    FilledTonalButton(
        onClick = LocalWalletOperationLaunchers.current.let { { it.openUnlockWallet(null) } },
    ) {
        Icon(
            TablerIcons.Lock,
            contentDescription = "Locked wallet",
            Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))

        Text("Open wallet")
    }
}
