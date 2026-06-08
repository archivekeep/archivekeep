package org.archivekeep.app.core.persistence.platform.demo

val DocumentsInLaptopSSD: DemoApplicationServices.MockedRepository
    get() = Documents.inStorage(LaptopSSD.reference)

val DocumentsInHDDA: DemoApplicationServices.MockedRepository
    get() = Documents.inStorage(hddA.reference)

val DocumentsInHDDB: DemoApplicationServices.MockedRepository
    get() = Documents.inStorage(hddB.reference)

val DocumentsInSSDKeyChain: DemoApplicationServices.MockedRepository
    get() = Documents.inStorage(ssdKeyChain.reference)

val DocumentsInBackBlaze: DemoApplicationServices.MockedRepository
    get() = Documents.inStorage(BackBlaze.reference)

val PhotosInLaptopSSD: DemoApplicationServices.MockedRepository
    get() = Photos.inStorage(LaptopSSD.reference)

val PhotosInHDDA: DemoApplicationServices.MockedRepository
    get() = Photos.inStorage(hddA.reference)

val PhotosInHDDB: DemoApplicationServices.MockedRepository
    get() = Photos.inStorage(hddB.reference)
