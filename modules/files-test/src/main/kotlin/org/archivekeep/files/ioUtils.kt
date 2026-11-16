package org.archivekeep.files

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.attribute.FileTime

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

fun File.writeTextKeepTimestamp(text: String) {
    val originalTime: FileTime? =
        takeIf { it.exists() }
            ?.let { Files.getAttribute(it.toPath(), "basic:lastModifiedTime") as? FileTime }

    writeText(text)

    originalTime?.let {
        Files.setAttribute(toPath(), "basic:lastModifiedTime", it)
    }
}
