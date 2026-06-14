package org.archivekeep.app.ui.utils.env

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.ui.domain.wiring.ApplicationServices

interface DemoTestEnvironment {
    val scope: CoroutineScope
    val demo: DemoApplicationServices
    val services: ApplicationServices
}
