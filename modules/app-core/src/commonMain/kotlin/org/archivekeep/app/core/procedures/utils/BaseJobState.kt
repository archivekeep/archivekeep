package org.archivekeep.app.core.procedures.utils

import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface BaseJobState {
    val progressLog: StateFlow<List<String>>
    val executionState: ProcedureExecutionState
}
