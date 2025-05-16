package org.archivekeep.app.core.procedures.add

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface IndexUpdateProcedureSupervisorService {
    fun getAddOperation(repositoryURI: RepositoryURI): IndexUpdateProcedureSupervisor
}
