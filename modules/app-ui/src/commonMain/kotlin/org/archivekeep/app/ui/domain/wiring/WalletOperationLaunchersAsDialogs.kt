package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.archivekeep.app.ui.dialogs.wallet.CreateWalletDialog
import org.archivekeep.app.ui.dialogs.wallet.UnlockWalletDialog
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorage

@Composable
fun rememberWalletOperationLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer): WalletOperationLaunchers {
    val walletDataStore = LocalApplicationServices.current.environment.walletDataStore

    val walletOperationLaunchers =
        remember(walletDataStore) {
            WalletOperationLaunchers(
                ensureWalletForWrite = {
                    if (!walletDataStore.needsUnlock()) {
                        return@WalletOperationLaunchers true
                    }

                    val state =
                        (walletDataStore as PasswordProtectedJoseStorage)
                            .autoloadFlow
                            .filter { it !is PasswordProtectedJoseStorage.State.NotInitialized }
                            .first()

                    if (state is PasswordProtectedJoseStorage.State.NotExisting) {
                        // TODO: implement open dialog for result, or something like that, and await,...
                        // or, rework to assert check and "open or create launcher" with separate UI button
                        dialogRenderer.openDialog(CreateWalletDialog())
                        false
                    } else if (state is PasswordProtectedJoseStorage.State.Locked) {
                        // TODO: implement open dialog for result, or something like that, and await
                        // or, rework to assert check and "open or create launcher" with separate UI button
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
