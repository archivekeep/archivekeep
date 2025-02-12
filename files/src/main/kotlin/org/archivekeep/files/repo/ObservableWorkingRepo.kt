package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import org.archivekeep.files.operations.StatusOperation

interface ObservableWorkingRepo : ObservableRepo {
    val localIndex: Flow<StatusOperation.Result>
}
