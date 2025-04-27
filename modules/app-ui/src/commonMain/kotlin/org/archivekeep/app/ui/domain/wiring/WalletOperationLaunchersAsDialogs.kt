package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.ui.dialogs.wallet.CreateWalletDialog
import org.archivekeep.app.ui.dialogs.wallet.UnlockWalletDialog

@Composable
fun rememberWalletOperationLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer): WalletOperationLaunchers {
    val joseStorage = LocalWalletDataStore.current

    val walletOperationLaunchers =
        remember(joseStorage) {
            WalletOperationLaunchers(
                ensureWalletForWrite = {
                    val state =
                        joseStorage.autoloadFlow
                            .filter { it !is JoseStorage.State.NotInitialized }
                            .first()

                    if (state is JoseStorage.State.NotExisting) {
                        dialogRenderer.openDialog(CreateWalletDialog())
                        false
                    } else if (state is JoseStorage.State.Locked) {
                        dialogRenderer.openDialog(UnlockWalletDialog(onUnlock = {}))
                        false
                    } else {
                        true
                    }
                },
                openUnlockWallet = { onUnlock ->
                    dialogRenderer.openDialog(UnlockWalletDialog(onUnlock))
                },
            )
        }
    return walletOperationLaunchers
}
