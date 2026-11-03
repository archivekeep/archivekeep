package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import maryk.core.models.RootDataModel
import maryk.core.properties.definitions.number
import maryk.core.properties.definitions.string
import maryk.core.properties.types.numeric.SInt64
import maryk.core.values.Values
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import java.util.*

object FileModel : RootDataModel<FileModel>() {
    val path by string(index = 1u, unique = true)
    val fileSize by number(index = 2u, type = SInt64)
    val checksumSha256 by string(index = 3u)
    val lastModified by number(index = 4u, type = SInt64)
}

internal fun Values<FileModel>.toDatabaseFileRecord(): DatabaseIndexStore.DatabaseFileRecord =
    DatabaseIndexStore.DatabaseFileRecord(
        path = this { FileModel.path }!!,
        size = this { FileModel.fileSize }!!,
        checksumSha256 = this { FileModel.checksumSha256 }!!,
        lastModified = Date(this { FileModel.lastModified }!!),
    )
