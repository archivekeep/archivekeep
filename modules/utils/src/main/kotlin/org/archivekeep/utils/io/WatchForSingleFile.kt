package org.archivekeep.utils.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.absolute

fun (Path).watchForSingleFile(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    pollDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Flow<List<CustomWatchEvent>> =
    flow {
        val watchService =
            withContext(ioDispatcher) {
                fileSystem.newWatchService()
            }

        try {
            suspend fun (Path).register() {
                withContext(ioDispatcher) {
                    register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.OVERFLOW,
                        StandardWatchEventKinds.ENTRY_DELETE,
                    )
                }
            }

            this@watchForSingleFile.parent.register()

            emitAll(
                watchService
                    .eventFlow(pollDispatcher)
                    .mapToCustomWatchEvents()
                    .map { list ->
                        list.filter {
                            when (it) {
                                is CustomWatchEvent.Started -> true
                                is CustomWatchEvent.Change -> {
                                    it.subject.absolute() == this@watchForSingleFile.absolute()
                                }
                                is CustomWatchEvent.Overflow -> true
                            }
                        }
                    }.filter { it.isNotEmpty() },
            )
        } finally {
            withContext(NonCancellable) {
                watchService.close()
            }
        }
    }
