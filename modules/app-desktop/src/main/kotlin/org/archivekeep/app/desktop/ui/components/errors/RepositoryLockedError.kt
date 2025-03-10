package org.archivekeep.app.desktop.ui.components.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.desktop.domain.data.canUnlock
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers

@Composable
fun RepositoryLockedError(
    cause: RepositoryLockedException,
    onResolve: () -> Unit,
) {
    val repoOperationLaunchers = LocalArchiveOperationLaunchers.current
    val walletOperationLaunchers = LocalWalletOperationLaunchers.current
    val credentialStorage = LocalWalletDataStore.current

    Text("Repository ${cause.uri} is locked")

    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (credentialStorage.canUnlock()) {
            OutlinedButton(
                onClick = { walletOperationLaunchers.openUnlockWallet(onResolve) },
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

        OutlinedButton(onClick = {
            repoOperationLaunchers.unlockRepository(cause.uri, onResolve)
        }) {
            Text("Unlock")
        }
    }
}
