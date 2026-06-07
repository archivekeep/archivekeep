package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
import org.archivekeep.app.core.persistence.platform.Environment

fun createApplicationServices(
    serviceWorkDispatcher: CoroutineDispatcher,
    basescope: CoroutineScope,
    environment: Environment,
): ApplicationServicesGraph {
    val scope = basescope + serviceWorkDispatcher

    return createGraphFactory<ApplicationServicesGraph.Factory>().create(scope, serviceWorkDispatcher, environment)
}
