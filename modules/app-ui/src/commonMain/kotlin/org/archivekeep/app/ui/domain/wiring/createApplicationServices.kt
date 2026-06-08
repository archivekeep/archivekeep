package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.createGraphFactory
import org.archivekeep.app.core.domain.CoreApplicationServices

fun createApplicationServices(coreApplicationServices: CoreApplicationServices): ApplicationServices =
    createGraphFactory<ApplicationServices.FromCore.Factory>().create(coreApplicationServices)
