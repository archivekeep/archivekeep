package org.archivekeep.app.core.domain.repositories

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface RepositoryService {
    fun getRepository(repositoryURI: RepositoryURI): Repository

    suspend fun registerRepository(repositoryURI: RepositoryURI)
}
