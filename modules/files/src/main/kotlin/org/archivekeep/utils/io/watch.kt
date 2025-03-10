package org.archivekeep.utils.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

fun (Path).watch(ioDispatcher: CoroutineDispatcher = Dispatchers.IO): Flow<Any> =
    flow {
        val watchService =
            withContext(ioDispatcher) {
                this@watch.fileSystem.newWatchService()
            }

        try {
            this@watch.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.OVERFLOW,
                StandardWatchEventKinds.ENTRY_DELETE,
            )

            emitAll(
                watchService.eventFlow(),
            )
        } finally {
            withContext(NonCancellable) {
                watchService.close()
            }
        }
    }.flowOn(ioDispatcher)
