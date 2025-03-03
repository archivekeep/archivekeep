package org.archivekeep.app.core.persistence.platform.demo

val DocumentsInLaptopSSD: DemoEnvironment.MockedRepository
    get() = Documents.inStorage(LaptopSSD.reference)

val DocumentsInHDDA: DemoEnvironment.MockedRepository
    get() = Documents.inStorage(hddA.reference)

val DocumentsInHDDB: DemoEnvironment.MockedRepository
    get() = Documents.inStorage(hddB.reference)
