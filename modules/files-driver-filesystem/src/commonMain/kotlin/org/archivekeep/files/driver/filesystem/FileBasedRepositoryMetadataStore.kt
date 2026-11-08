package org.archivekeep.files.driver.filesystem

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.produceLoadableStateIn
import org.archivekeep.utils.safeFileReadWithChecksum
import org.archivekeep.utils.safeFileReadWriteWithChecksum
import java.nio.file.Path
import kotlin.time.Duration

class FileBasedRepositoryMetadataStore(
    private val metadataPath: Path,
    private val rootExists: () -> Boolean,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val throttlePauseDuration: Duration,
) {
    private suspend fun readMetadataContent(): String? =
        safeFileReadWithChecksum(metadataPath) {
            if (rootExists()) {
                Json.encodeToString(RepositoryMetadata())
            } else {
                null
            }
        }

    private fun decodeMetadata(content: String): RepositoryMetadata =
        try {
            Json.decodeFromString<RepositoryMetadata>(content)
        } catch (e: Throwable) {
            println("WARNING: Corrupted metadata.json at $metadataPath, falling back to defaults. Cause: ${e.message}")
            RepositoryMetadata()
        }

    suspend fun getMetadata(): RepositoryMetadata =
        withContext(Dispatchers.IO) {
            val content = readMetadataContent()
            if (content == null) {
                if (rootExists()) {
                    RepositoryMetadata()
                } else {
                    throw RuntimeException("Something went wrong")
                }
            } else {
                decodeMetadata(content)
            }
        }

    private suspend fun readCurrentMetadataForUpdate(): RepositoryMetadata {
        val content = safeFileReadWithChecksum(metadataPath) { null }
        return try {
            content?.let { Json.decodeFromString<RepositoryMetadata>(it) } ?: RepositoryMetadata()
        } catch (e: Throwable) {
            println("WARNING: Corrupted metadata.json at $metadataPath during update, falling back to defaults. Cause: ${e.message}")
            RepositoryMetadata()
        }
    }

    suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        val old = readCurrentMetadataForUpdate()
        val newContent = Json.encodeToString(transform(old))
        safeFileReadWriteWithChecksum(metadataPath, newContent)
    }

    val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        metadataPath
            .produceLoadableStateIn(
                scope,
                watchDispatcher = ioDispatcher,
                workDispatcher = ioDispatcher,
                message = "Repository metadata: $metadataPath",
                throttle = throttlePauseDuration,
            ) {
                val content = readMetadataContent()
                if (content == null) {
                    if (rootExists()) {
                        RepositoryMetadata()
                    } else {
                        throw RuntimeException("Something went wrong")
                    }
                } else {
                    decodeMetadata(content)
                }
            }
}
