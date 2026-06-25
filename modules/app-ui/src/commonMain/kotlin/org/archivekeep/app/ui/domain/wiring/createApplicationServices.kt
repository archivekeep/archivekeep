package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.createGraphFactory
import org.archivekeep.app.core.domain.CoreApplicationServices
import org.archivekeep.app.ui.utils.ApplicationMetadata

fun createApplicationServices(
    coreApplicationServices: CoreApplicationServices,
    applicationMetadata: ApplicationMetadata,
): ApplicationServices = createGraphFactory<ApplicationServices.FromCore.Factory>().create(coreApplicationServices, applicationMetadata)
