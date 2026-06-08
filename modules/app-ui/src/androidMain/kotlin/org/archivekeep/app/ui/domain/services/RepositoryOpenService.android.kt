package org.archivekeep.app.ui.domain.services

import org.archivekeep.app.ui.domain.wiring.ApplicationServices

actual fun createRepositoryOpenService(applicationServices: ApplicationServices): RepositoryOpenService = AndroidRepositoryOpenService()
