package org.archivekeep.app.core.procedures.deletedcleanup

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface DeletedFilesCleanupProcedureSupervisorService {
    fun getDeletedFilesCleanupOperation(repositoryURI: RepositoryURI): DeletedFilesCleanupProcedureSupervisor
}
