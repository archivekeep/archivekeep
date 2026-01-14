package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageInformation.Partition.DriveType

val Documents = DemoEnvironment.DemoRepository("Documents").withContents(documentsContents)
val Photos = DemoEnvironment.DemoRepository("Photos").withContents(photosBaseContents)
val Music = DemoEnvironment.DemoRepository("Music").withContents(musicBaseContents)
val Private = DemoEnvironment.DemoRepository("Private").withContents(privateBaseContents)
val Books = DemoEnvironment.DemoRepository("E-Books").withContents(booksBaseContents)
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
        isLocal = true,
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
        isLocal = false,
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
        isLocal = true,
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
        isLocal = false,
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
        isLocal = false,
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
        isLocal = false,
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
        isLocal = true,
        connectionStatus = Storage.ConnectionStatus.DISCONNECTED,
        repositories =
            listOf(
                Documents.localInMemoryFactory(),
                Music
                    .withContents {
                        deletePattern(".*/1[0-9].ogg".toRegex())
                    }.localInMemoryFactory(),
                Books.localInMemoryFactory(),
            ),
    )

val usbStickAll =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "USB Stick - All",
        isLocal = false,
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents,
                Music,
                Books,
            ),
    )

val usbStickAllUnassociated =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "USB Stick - All (unassociated)",
        isLocal = false,
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents.copy(
                    correlationId = null,
                ),
                Music.copy(
                    correlationId = null,
                ),
                Books.copy(
                    correlationId = null,
                ),
            ),
    )

val usbStickDocuments =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "USB Stick - Documents",
        isLocal = false,
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Documents,
            ),
    )

val usbStickMusic =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.HDD,
        displayName = "USB Stick - Music",
        isLocal = false,
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories =
            listOf(
                Music,
            ),
    )

val eBook =
    DemoEnvironment.DemoPhysicalMedium(
        physicalID = "TODO",
        driveType = DriveType.Other,
        displayName = "EBook",
        isLocal = false,
        connectionStatus = Storage.ConnectionStatus.DISCONNECTED,
        repositories =
            listOf(
                Books,
                Music,
            ),
    )

val BackBlaze =
    DemoEnvironment.DemoOnlineStorage(
        displayName = "Backblaze S3",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )

val NAS =
    DemoEnvironment.DemoOnlineStorage(
        displayName = "NAS",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )
