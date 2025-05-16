package org.archivekeep.app.core.procedures.addpush

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface AddAndPushProcedureService {
    fun getAddAndPushProcedure(repositoryURI: RepositoryURI): AddAndPushProcedure
}
