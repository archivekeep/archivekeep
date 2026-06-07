package org.archivekeep.app.ui.domain.services

import org.archivekeep.app.ui.domain.wiring.ApplicationServicesGraph

actual fun createRepositoryOpenService(applicationServices: ApplicationServicesGraph): RepositoryOpenService = AndroidRepositoryOpenService()
