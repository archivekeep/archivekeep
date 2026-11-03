package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import maryk.core.models.RootDataModel
import maryk.core.properties.definitions.number
import maryk.core.properties.definitions.string
import maryk.core.properties.types.numeric.SInt64
import maryk.core.values.Values
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore

object IncomingFileModel : RootDataModel<IncomingFileModel>() {
    val path by string(index = 1u, unique = true)
    val tmpWritePath by string(index = 2u)
    val fileSize by number(index = 3u, type = SInt64)
    val checksumSha256 by string(index = 4u)
    val lastAlive by number(index = 5u, type = SInt64)
}

internal fun Values<IncomingFileModel>.toIncomingFileRecord(): DatabaseIndexStore.IncomingFileRecord =
    DatabaseIndexStore.IncomingFileRecord(
        path = this { IncomingFileModel.path }!!,
        tmpWritePath = this { IncomingFileModel.tmpWritePath }!!,
        size = this { IncomingFileModel.fileSize }!!,
        checksumSha256 = this { IncomingFileModel.checksumSha256 }!!,
    )
