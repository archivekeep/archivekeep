package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import org.archivekeep.app.core.domain.CoreApplicationServices
import org.archivekeep.app.ui.utils.ApplicationMetadata

interface ApplicationServices : CoreApplicationServices {
    val operationFactory: OperationFactory

    val applicationMetadata: ApplicationMetadata

    @DependencyGraph(AppScope::class)
    interface FromCore : ApplicationServices {
        @DependencyGraph.Factory
        fun interface Factory {
            fun create(
                @Includes core: CoreApplicationServices,
                @Provides applicationMetadata: ApplicationMetadata,
            ): FromCore
        }
    }
}
