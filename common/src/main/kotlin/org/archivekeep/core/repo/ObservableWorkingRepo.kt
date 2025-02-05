package org.archivekeep.core.repo

import kotlinx.coroutines.flow.Flow
import org.archivekeep.core.operations.StatusOperation

interface ObservableWorkingRepo : ObservableRepo {
    val localIndex: Flow<StatusOperation.Result>
}
