package org.archivekeep.app.desktop.domain.services

import org.archivekeep.app.desktop.domain.wiring.ApplicationServices

actual fun createRepositoryOpenService(applicationServices: ApplicationServices): RepositoryOpenService = AndroidRepositoryOpenService()
