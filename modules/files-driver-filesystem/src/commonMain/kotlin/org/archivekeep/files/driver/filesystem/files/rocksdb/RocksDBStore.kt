package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.RocksDB
import maryk.rocksdb.RocksDBException
import org.rocksdb.FlushOptions

class RocksDBStore(
    val db: RocksDB,
    val cfFiles: ColumnFamilyHandle,
    val cfIncomingFiles: ColumnFamilyHandle,
    val cfPendingMoves: ColumnFamilyHandle,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _filesChanged =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val filesChanged: SharedFlow<Unit> = _filesChanged

    private val flushOptions =
        FlushOptions().apply {
            setWaitForFlush(true)
            setAllowWriteStall(false)
        }

    private val allCfHandles = listOf(cfFiles, cfIncomingFiles, cfPendingMoves)

    private val flushRequested =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val flushJob =
        scope.launch(dispatcher) {
            flushRequested.collect {
                try {
                    db.flush(flushOptions, allCfHandles)
                } catch (e: RocksDBException) {
                    println("[$storeTag] flush: FAILED — ${e.message}")
                    e.printStackTrace()
                }
            }
        }

    fun flush() {
        flushRequested.tryEmit(Unit)
    }

    suspend fun notifyFilesChanged() {
        _filesChanged.tryEmit(Unit)
    }

    fun close() {
        flushJob.cancel()
        flushOptions.close()
        db.close()
    }
}
