package org.archivekeep.app.core.procedures.verify

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface VerifySupervisorService {
    fun getVerifyOperation(repositoryURI: RepositoryURI): VerifySupervisor
}
