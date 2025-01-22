package org.archivekeep.app.core.persistence.platform.demo

data class Preset(
    val title: String,
    val physicalMediaData: List<DemoEnvironment.DemoPhysicalMedium>,
    val onlineStoragesData: List<DemoEnvironment.DemoOnlineStorage>,
)

val fullComplexPreset =
    Preset(
        title = "Full (big)",
        physicalMediaData =
            listOf(
                LaptopSSD,
                LaptopHDD,
                hddA,
                hddB,
                hddC,
                phone,
                eBook,
            ),
        onlineStoragesData =
            listOf(
                BackBlaze,
                NAS,
            ),
    )
