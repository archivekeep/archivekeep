package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageInformation.Partition.DriveType

val Documents = DemoApplicationServices.DemoRepository("Documents").withContents(documentsContents)
val Photos = DemoApplicationServices.DemoRepository("Photos").withContents(photosBaseContents)
val Music = DemoApplicationServices.DemoRepository("Music").withContents(musicBaseContents)
val Private = DemoApplicationServices.DemoRepository("Private").withContents(privateBaseContents)
val Books = DemoApplicationServices.DemoRepository("E-Books").withContents(booksBaseContents)
val Productions =
    DemoApplicationServices.DemoRepository("Productions").withContents(productionsBaseContents)

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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoPhysicalMedium(
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
    DemoApplicationServices.DemoOnlineStorage(
        displayName = "Backblaze S3",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )

val NAS =
    DemoApplicationServices.DemoOnlineStorage(
        displayName = "NAS",
        connectionStatus = Storage.ConnectionStatus.CONNECTED,
        repositories = allArchives,
    )
