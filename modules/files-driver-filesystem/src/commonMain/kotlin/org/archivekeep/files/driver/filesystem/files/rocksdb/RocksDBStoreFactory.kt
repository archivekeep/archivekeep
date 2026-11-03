package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import maryk.rocksdb.ColumnFamilyDescriptor
import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.ColumnFamilyOptions
import maryk.rocksdb.CompressionType
import maryk.rocksdb.DBOptions
import maryk.rocksdb.RocksDBException
import maryk.rocksdb.defaultColumnFamily
import maryk.rocksdb.loadRocksDBLibrary
import maryk.rocksdb.openRocksDB
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileColumnFamily
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileColumnFamily
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveColumnFamily

internal val rocksDbJson = Json { ignoreUnknownKeys = true }

const val storeTag = "RocksDBStore"

internal fun openRocksDBStore(
    path: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): RocksDBStore {
    loadRocksDBLibrary()

    val dbOptions =
        DBOptions().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }

    val cfOptions =
        ColumnFamilyOptions().apply {
            setCompressionType(CompressionType.LZ4_COMPRESSION)
            setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION)
            setCompressionPerLevel(
                listOf(
                    CompressionType.LZ4_COMPRESSION, // L0
                    CompressionType.LZ4_COMPRESSION, // L1
                    CompressionType.LZ4_COMPRESSION, // L2
                    CompressionType.LZ4_COMPRESSION, // L3
                    CompressionType.ZSTD_COMPRESSION, // L4
                    CompressionType.ZSTD_COMPRESSION, // L5
                    CompressionType.ZSTD_COMPRESSION, // L6
                ),
            )
        }

    val cfDescriptors =
        listOf(
            ColumnFamilyDescriptor(defaultColumnFamily, cfOptions),
            ColumnFamilyDescriptor(FileColumnFamily.encodeToByteArray(), cfOptions),
            ColumnFamilyDescriptor(IncomingFileColumnFamily.encodeToByteArray(), cfOptions),
            ColumnFamilyDescriptor(PendingMoveColumnFamily.encodeToByteArray(), cfOptions),
        )

    val cfHandles = mutableListOf<ColumnFamilyHandle>()

    val database =
        try {
            openRocksDB(dbOptions, path, cfDescriptors, cfHandles)
        } catch (e: RocksDBException) {
            throw RuntimeException("Failed to open RocksDB at $path", e)
        }

    // default CF is at index 0, our column families start at 1
    val cfFiles = cfHandles[1]
    val cfIncomingFiles = cfHandles[2]
    val cfPendingMoves = cfHandles[3]

    cfOptions.close()
    dbOptions.close()

    return RocksDBStore(database, cfFiles, cfIncomingFiles, cfPendingMoves, scope, dispatcher)
}
