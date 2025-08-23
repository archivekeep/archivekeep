package org.archivekeep.app.ui.domain.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedDataStore
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorageInFile
import org.archivekeep.utils.loading.ProtectedLoadableResource

fun(PasswordProtectedDataStore<WalletPO>?).canUnlockFlow() =
    (this as? PasswordProtectedJoseStorageInFile)?.data?.map {
        it is ProtectedLoadableResource.PendingAuthentication
    } ?: flowOf(false)

@Composable
fun (PasswordProtectedDataStore<WalletPO>?).canUnlock() =
    (this as? PasswordProtectedJoseStorageInFile)
        ?.autoloadFlow
        ?.collectAsState()
        ?.value is PasswordProtectedJoseStorageInFile.State.Locked
