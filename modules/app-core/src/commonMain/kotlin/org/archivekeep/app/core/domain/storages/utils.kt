package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

fun Map<String, StorageDriver>.getDriverForURI(repositoryURI: RepositoryURI): StorageDriver? = this[repositoryURI.driver]
