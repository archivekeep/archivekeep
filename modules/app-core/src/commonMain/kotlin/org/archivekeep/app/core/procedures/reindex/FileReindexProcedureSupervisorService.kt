package org.archivekeep.app.core.procedures.reindex

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface FileReindexProcedureSupervisorService {
    fun getFileReindexOperation(repositoryURI: RepositoryURI): FileReindexProcedureSupervisor
}
