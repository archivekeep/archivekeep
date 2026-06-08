package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import org.archivekeep.app.core.domain.CoreApplicationServices

interface ApplicationServices : CoreApplicationServices {
    val operationFactory: OperationFactory

    @DependencyGraph(AppScope::class)
    interface FromCore : ApplicationServices {
        @DependencyGraph.Factory
        fun interface Factory {
            fun create(
                @Includes core: CoreApplicationServices,
            ): FromCore
        }
    }
}
