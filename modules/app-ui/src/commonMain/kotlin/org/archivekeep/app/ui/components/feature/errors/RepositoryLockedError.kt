package org.archivekeep.app.ui.components.feature.errors

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
import org.archivekeep.app.ui.domain.data.canUnlock
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_unlock
import org.archivekeep.ui.resources.dialog_unlock_wallet_locked
import org.archivekeep.ui.resources.dialog_unlock_wallet_open
import org.archivekeep.ui.resources.error_repository_locked
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedDataStore
import org.jetbrains.compose.resources.stringResource

@Composable
fun RepositoryLockedError(
    cause: RepositoryLockedException,
    onResolve: () -> Unit,
) {
    val repoOperationLaunchers = LocalArchiveOperationLaunchers.current
    val walletOperationLaunchers = LocalWalletOperationLaunchers.current
    val credentialStorage = LocalApplicationServices.current.walletDataStore as? PasswordProtectedDataStore

    Text(stringResource(Res.string.error_repository_locked, cause.uri))

    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (credentialStorage.canUnlock()) {
            OutlinedButton(
                onClick = { walletOperationLaunchers.openUnlockWallet(onResolve) },
            ) {
                Icon(
                    TablerIcons.Lock,
                    contentDescription = stringResource(Res.string.dialog_unlock_wallet_locked),
                    Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))

                Text(stringResource(Res.string.dialog_unlock_wallet_open))
            }
        }

        OutlinedButton(onClick = {
            repoOperationLaunchers.unlockRepository(cause.uri, onResolve)
        }) {
            Text(stringResource(Res.string.common_unlock))
        }
    }
}
