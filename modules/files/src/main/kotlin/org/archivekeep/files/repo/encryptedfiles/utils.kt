package org.archivekeep.files.repo.encryptedfiles

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.archivekeep.files.exceptions.ChecksumMismatch
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
fun CoroutineScope.verifyingStreamViaBackgroundCoroutine(
    stream: InputStream,
    monitor: (copiedBytes: Long) -> Unit,
    expectedChecksum: String,
    blockingIOPopulateDispatcher: CoroutineDispatcher = Dispatchers.IO,
    checksumComputeDispatcher: CoroutineDispatcher = Dispatchers.Default,
): PipedInputStream {
    val digest = MessageDigest.getInstance("SHA-256")

    val pipedInputStream = PipedInputStream()
    val pipedOutputStream = PipedOutputStream(pipedInputStream)

    launch(blockingIOPopulateDispatcher) {
        val buffer = ByteArray(2 * 1024 * 1024)
        var read: Int = stream.read(buffer)
        var total: Long = 0
        var lastSync: Long = 0

        while (read != -1) {
            // check the job is active
            currentCoroutineContext().ensureActive()

            coroutineScope {
                launch(checksumComputeDispatcher) { digest.update(buffer, 0, read) }
                runInterruptible {
                    pipedOutputStream.write(buffer, 0, read)
                }
            }

            total += read

            read = stream.read(buffer)

            if (total - lastSync > 25 * 1024 * 1024) {
                // TODO: output.force(false)
                lastSync = total
                monitor(total)
            }
        }

        // TODO: output.force(true)
        monitor(total)

        if (digest.digest().toHexString() != expectedChecksum) {
            throw ChecksumMismatch(expectedChecksum, digest.digest().toHexString())
        }

        pipedOutputStream.close()
    }
    return pipedInputStream
}
