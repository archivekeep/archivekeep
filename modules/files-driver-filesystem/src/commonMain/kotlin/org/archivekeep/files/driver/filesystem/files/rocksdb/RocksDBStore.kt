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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class RocksDBStore(
    private val db: RocksDB,
    val cfFiles: ColumnFamilyHandle,
    val cfIncomingFiles: ColumnFamilyHandle,
    val cfPendingMoves: ColumnFamilyHandle,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val lifecycleLock = ReentrantReadWriteLock()
    private var closed = false
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
                    lifecycleLock.read {
                        if (!closed) {
                            db.flush(flushOptions, allCfHandles)
                        }
                    }
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

    internal fun <T> withDbAccess(action: RocksDB.() -> T): T =
        lifecycleLock.read {
            check(!closed) { "RocksDB store is closed" }
            db.action()
        }

    fun close() {
        flushJob.cancel()
        lifecycleLock.write {
            if (closed) return@write

            closed = true
            try {
                allCfHandles.forEach { it.close() }
            } finally {
                try {
                    flushOptions.close()
                } finally {
                    db.close()
                }
            }
        }
    }
}
