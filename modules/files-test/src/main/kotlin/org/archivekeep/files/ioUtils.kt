package org.archivekeep.files

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

fun (Flow<ByteArray>).flowToInputStream(scope: CoroutineScope): InputStream {
    val outputStream = PipedOutputStream()
    val inputStream = PipedInputStream(outputStream)

    scope.launch {
        try {
            collect { chunk ->
                outputStream.write(chunk)
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    return inputStream
}
