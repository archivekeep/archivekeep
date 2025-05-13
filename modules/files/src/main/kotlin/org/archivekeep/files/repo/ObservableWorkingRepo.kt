package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.utils.loading.Loadable

interface ObservableWorkingRepo : ObservableRepo {
    val localIndex: Flow<Loadable<StatusOperation.Result>>
}
