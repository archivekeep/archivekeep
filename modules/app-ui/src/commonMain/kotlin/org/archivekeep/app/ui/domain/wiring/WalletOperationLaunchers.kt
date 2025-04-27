package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf

data class WalletOperationLaunchers(
    val ensureWalletForWrite: suspend () -> Boolean,
    val openUnlockWallet: (onUnlock: (() -> Unit)?) -> Unit,
)

val LocalWalletOperationLaunchers =
    staticCompositionLocalOf {
        WalletOperationLaunchers(
            ensureWalletForWrite = { invalidUseOfContext("ensureWalletForWrite") },
            openUnlockWallet = { invalidUseOfContext("openUnlockWallet") },
        )
    }

private fun invalidUseOfContext(name: String): Nothing = throw Error("Context must be present to call $name")
