package org.archivekeep.app.ui.utils.env

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.core.persistence.platform.demo.DemoOnlineStorage
import org.archivekeep.app.core.persistence.platform.demo.DemoPhysicalMedium
import org.archivekeep.app.core.persistence.platform.demo.LaptopHDD
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.core.persistence.platform.demo.hddC
import org.archivekeep.app.ui.domain.services.NoOpRepositoryOpenService
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.screenshots.runHighDensityComposeUiTest

@OptIn(ExperimentalTestApi::class)
fun runHighDensityComposeUiTestWithDemoEnv(
    physicalMediaData: List<DemoPhysicalMedium> = listOf(LaptopSSD, LaptopHDD, hddB, hddC),
    onlineStoragesData: List<DemoOnlineStorage> = emptyList(),
    mountPoints: List<MountedFileSystem.MountPoint> = emptyList(),
    storagesOverride: List<StorageDriver>? = null,
    sizeNotScaled: Size = Size(1200.0f, 800.0f),
    factory: DemoApplicationServices.Factory = createGraphFactory<DemoApplicationServices.Factory>(),
    block: SkikoComposeUiTest.(env: DemoTestEnvironment) -> Unit,
) {
    val job = SupervisorJob()
    val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()

    try {
        val env =
            object : DemoTestEnvironment {
                override val scope = CoroutineScope(job)

                override val demo =
                    factory.create(
                        scope,
                        serviceWorkDispatcher = serviceWorkDispatcher,
                        physicalMediaData = physicalMediaData,
                        onlineStoragesData = onlineStoragesData,
                        enableSpeedLimit = false,
                        mountPoints = mountPoints,
                        storagesOverride = storagesOverride,
                    )

                override val services =
                    createGraphFactory<ApplicationServices.FromCore.Factory>().create(demo, PropertiesApplicationMetadata(), NoOpRepositoryOpenService)
            }

        runHighDensityComposeUiTest(sizeNotScaled) {
            block(env)
        }
    } finally {
        runBlocking {
            job.cancelAndJoin()
            serviceWorkDispatcher.cancel()
        }
    }
}
