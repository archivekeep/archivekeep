package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageInformation.Partition.DriveType

val Documents = DemoEnvironment.DemoRepository("Documents").withContents(documentsContents)
val Photos = DemoEnvironment.DemoRepository("Photos").withContents(photosBaseContents)
val Music = DemoEnvironment.DemoRepository("Music").withContents(musicBaseContents)
val Private = DemoEnvironment.DemoRepository("Private").withContents(privateBaseContents)
val Books = DemoEnvironment.DemoRepository("Books").withContents(booksBaseContents)
val Productions =
    DemoEnvironment.DemoRepository("Productions").withContents(productionsBaseContents)

val allArchives =
    listOf(
        Documents,
        Photos,
        Music,
        Private,
        Books,
        Productions,
    )

val LaptopSSD =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.SSD,
        displayName = "Laptop / SSD",
        logicalLocation = "localhost.ssd.home_partition",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents.localInMemoryFactory(),
                Private.localInMemoryFactory(),
                Music.localInMemoryFactory(),
                Books.localInMemoryFactory(),
            ),
    )

val hddB =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "HDD B",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents,
                Photos
                    .withContents {
                        deletePattern("2024/[4-7]/.*".toRegex())
                        moveToUncommitted("2024/3/.*".toRegex())
                        addUncommitted("2025/something-weird.jpg")
                    }.localInMemoryFactory(),
                Music,
                Private,
                Books,
                Productions,
            ),
    )

val LaptopHDD =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "Laptop / HDD",
        logicalLocation = "localhost.ssd2.partition_a",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Photos
                    .withContents {
                        // whoops, accidental delete
                        deletePattern("2021/.*".toRegex())
                    }.localInMemoryFactory(),
                Productions.localInMemoryFactory(),
            ),
    )
val hddC =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "HDD C",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.DISCONNECTED,
        repositories =
            listOf(
                Documents,
                Photos.withContents {
                    deletePattern("2024/[2-7]/.*".toRegex())
                },
                Music.withContents {
                    deletePattern(".*/1[0-9].ogg".toRegex())
                },
                Private,
                Books,
                Productions,
            ),
    )
val hddA =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "HDD A",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents,
                Photos.withContents {
                    deletePattern("2024/07/.*".toRegex())
                },
                Music,
                Private,
                Books,
                Productions,
            ),
    )

val ssdKeyChain =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "KeyChain SSD",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents,
                Photos.withContents {
                    deletePattern("2024/07/.*".toRegex())
                },
                Music,
                Private,
                Books,
                Productions,
            ),
    )

val phone =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.Other,
        displayName = "Phone",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.DISCONNECTED,
        repositories =
            listOf(
                Documents,
                Books,
                Music,
            ),
    )
val eBook =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.Other,
        displayName = "EBook",
        logicalLocation = "ip-address.archivekeep-server",
        connectionStatus = Storage.ConnectionStatus.DISCONNECTED,
        repositories =
            listOf(
                Books,
                Music,
            ),
    )

val BackBlaze =
    DemoEnvironment.DemoOnlineStorage(
        displayName = "Backblaze S3 (planned)",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )

val NAS =
    DemoEnvironment.DemoOnlineStorage(
        displayName = "NAS",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )
