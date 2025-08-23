package org.archivekeep.files.driver.s3

import aws.smithy.kotlin.runtime.content.toByteStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import java.io.InputStream

fun CoroutineScope.interruptibleSourceFromInputStream(stream: InputStream) =
    flow {
        val buffer = ByteArray(64 * 1024)
        var read: Int = runInterruptible { stream.read(buffer) }

        while (read != -1) {
            ensureActive()

            if (read != buffer.size) {
                emit(buffer.copyOf(read))
            } else {
                emit(buffer)
            }

            read = runInterruptible { stream.read(buffer) }
        }
    }.toByteStream(this)
