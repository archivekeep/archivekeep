package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import java.util.*

class ObjectBoxDataSourceImpl(
    store: BoxStore,
) : AbstractObjectBoxDataSource(store) {
    override val fileBox = store.boxFor<FileEntity>()
    override val incomingFileBox = store.boxFor<IncomingFileEntity>()
    override val pendingMoveBox = store.boxFor<PendingMoveEntity>()

    override fun queryFileByPath(path: String): FileEntity? =
        fileBox
            .query(FileEntity_.path.equal(path))
            .build()
            .findFirst()

    override fun queryIncomingFileByPath(path: String): IncomingFileEntity? =
        incomingFileBox
            .query(IncomingFileEntity_.path.equal(path))
            .build()
            .findFirst()

    override fun queryPendingMoveByPath(to: String): PendingMoveEntity? =
        pendingMoveBox
            .query(PendingMoveEntity_.to.equal(to))
            .build()
            .findFirst()

    override fun queryIncomingFilesAliveBefore(date: Date): List<IncomingFileEntity> =
        incomingFileBox
            .query(IncomingFileEntity_.lastAlive.lessOrEqual(date))
            .build()
            .find()

    override fun queryPendingMovesAliveBefore(date: Date): List<PendingMoveEntity> =
        pendingMoveBox
            .query(PendingMoveEntity_.lastAlive.lessOrEqual(date))
            .build()
            .find()
}

actual fun createObjectBoxDataSource(store: BoxStore): ObjectBoxDataSource = ObjectBoxDataSourceImpl(store)
