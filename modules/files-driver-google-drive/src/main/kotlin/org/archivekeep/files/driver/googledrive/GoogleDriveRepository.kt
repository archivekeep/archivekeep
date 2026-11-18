package org.archivekeep.files.driver.googledrive

import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.api.exceptions.ChecksumMismatch
import org.archivekeep.files.api.exceptions.DestinationExists
import org.archivekeep.files.api.exceptions.FileDoesntExist
import org.archivekeep.files.api.repository.ARCHIVE_METADATA_FILENAME
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.performance.RepoPerformanceTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.ResourceLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Date

private data class GoogleDriveFileWithPath(
    val file: GoogleDriveFile,
    val path: String,
)

class GoogleDriveRepository private constructor(
    private val storage: GoogleDriveStorage,
    private val rootFolderId: String,
    private val sharingDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) : Repo {
    private val scope = CoroutineScope(sharingDispatcher + SupervisorJob())
    private val contentsLastChangeFlow = MutableStateFlow(Date())
    private val metadataLastChangeFlow = MutableStateFlow(Date())
    private val mutationMutex = Mutex()

    internal var metadataFolderId: String = ""
        private set

    private val indexResource =
        ResourceLoader(
            scope,
            ioDispatcher,
            updateTriggerFlow = contentsLastChangeFlow,
        ) {
            val files = listUserFiles()
            val duplicate = files.groupBy { it.path }.entries.firstOrNull { it.value.size > 1 }
            if (duplicate != null) {
                throw GoogleDriveAmbiguousFileException(duplicate.key)
            }

            RepoIndex(
                files
                    .map { fileWithPath ->
                        val file = fileWithPath.file
                        if (file.isGoogleAppDocument) {
                            throw UnsupportedOperationException("Google Drive native file '${fileWithPath.path}' is not supported")
                        }

                        RepoIndex.File(
                            path = fileWithPath.path,
                            size =
                                file.size
                                    ?: throw UnsupportedOperationException("File '${fileWithPath.path}' has no binary size"),
                            checksumSha256 =
                                file.sha256Checksum
                                    ?: throw UnsupportedOperationException("File '${fileWithPath.path}' has no SHA-256 checksum"),
                        )
                    }.sortedBy { it.path },
            )
        }

    private fun listUserFiles(): List<GoogleDriveFileWithPath> {
        val result = mutableListOf<GoogleDriveFileWithPath>()
        val foldersToProcess = ArrayDeque<Pair<String, String>>()
        foldersToProcess.add(rootFolderId to "")

        while (foldersToProcess.isNotEmpty()) {
            val (currentFolderId, currentPath) = foldersToProcess.removeFirst()
            val children = storage.listChildren(currentFolderId)

            for (child in children) {
                if (child.isMetadataFolder(METADATA_FOLDER_NAME)) {
                    continue
                }

                val childPath = if (currentPath.isEmpty()) child.name else "$currentPath/${child.name}"
                if (child.isFolder) {
                    foldersToProcess.add(child.id to childPath)
                } else {
                    result.add(GoogleDriveFileWithPath(child, childPath))
                }
            }
        }

        return result
    }

    override val indexFlow: StateFlow<Loadable<RepoIndex>> = indexResource.stateFlow

    private val metadataResource =
        ResourceLoader(
            scope,
            ioDispatcher,
            updateTriggerFlow = metadataLastChangeFlow,
        ) {
            readMetadataFromDrive() ?: RepositoryMetadata()
        }

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> = metadataResource.stateFlow

    override suspend fun index(): RepoIndex = indexResource.getFreshAndUpdateState()

    override suspend fun move(
        from: String,
        to: String,
        performanceTracker: RepoPerformanceTracker,
    ) {
        try {
            withContext(ioDispatcher) {
                mutationMutex.withLock {
                    checkNotExists(to)
                    val sourceFile = findFileByPath(from) ?: throw FileDoesntExist(from)
                    val toSlashIndex = to.lastIndexOf('/')
                    val (toParentFolder, toName) =
                        if (toSlashIndex >= 0) {
                            val parentPath = to.substring(0, toSlashIndex)
                            val name = to.substring(toSlashIndex + 1)
                            storage.ensureFolderPath(rootFolderId, parentPath) to name
                        } else {
                            rootFolderId to to
                        }
                    storage.renameFile(sourceFile.id, toName)
                    storage.moveFile(sourceFile.id, toParentFolder)
                }
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T {
        val (file, stream) =
            withContext(ioDispatcher) {
                val file = findFileByPath(filename) ?: throw FileDoesntExist(filename)
                val info = file.toArchiveFileInfo()
                info to storage.downloadFile(file.id)
            }

        return stream.use { block(file, it) }
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
        performanceTracker: RepoPerformanceTracker,
    ) {
        val (length, checksum, data) = readAndChecksum(stream, monitor)
        if (length != info.length || checksum != info.checksumSha256) {
            throw ChecksumMismatch(
                info.checksumSha256,
                checksum,
                "copied file has wrong length or checksum: got=$length bytes/$checksum, expected=${info.length} bytes/${info.checksumSha256}",
            )
        }

        withContext(ioDispatcher) {
            mutationMutex.withLock {
                checkNotExists(filename)
                val parentFolder = ensureFileParentFolder(filename)
                val fileName = filename.substringAfterLast('/')
                val stored = storage.uploadFile(parentFolder, fileName, ByteArrayInputStream(data), length)
                val actualChecksum = stored.sha256Checksum
                if (actualChecksum == null || actualChecksum != info.checksumSha256 || stored.size != info.length) {
                    storage.deleteFile(stored.id)
                    throw ChecksumMismatch(
                        info.checksumSha256,
                        actualChecksum ?: "missing",
                        "Drive stored file with unexpected length or checksum: got=${stored.size} bytes/$actualChecksum, expected=${info.length} bytes/${info.checksumSha256}",
                    )
                }
            }
        }

        contentsLastChangeFlow.update { Date() }
    }

    private fun readAndChecksum(
        input: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ): Triple<Long, String, ByteArray> {
        val digest = MessageDigest.getInstance("SHA-256")
        val baos = ByteArrayOutputStream()
        val readBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L

        while (true) {
            val bytesRead = input.read(readBuffer)
            if (bytesRead < 0) break
            if (bytesRead == 0) continue

            baos.write(readBuffer, 0, bytesRead)
            digest.update(readBuffer, 0, bytesRead)
            copiedBytes += bytesRead
            monitor(copiedBytes)
        }

        return Triple(copiedBytes, digest.digest().toHexString(), baos.toByteArray())
    }

    private fun checkNotExists(filename: String) {
        if (findFileByPath(filename) != null) {
            throw DestinationExists(filename)
        }
    }

    private fun findFileByPath(path: String): GoogleDriveFile? {
        val files = listUserFiles()
        val matches = files.filter { it.path == path }
        if (matches.size > 1) {
            throw GoogleDriveAmbiguousFileException(path)
        }
        return matches.singleOrNull()?.file
    }

    private fun ensureFileParentFolder(filename: String): String {
        val slashIndex = filename.lastIndexOf('/')
        if (slashIndex < 0) {
            return rootFolderId
        }
        val parentPath = filename.substring(0, slashIndex)
        return storage.ensureFolderPath(rootFolderId, parentPath)
    }

    override suspend fun delete(filename: String) {
        try {
            withContext(ioDispatcher) {
                mutationMutex.withLock {
                    val file = findFileByPath(filename) ?: throw FileDoesntExist(filename)
                    storage.deleteFile(file.id)
                }
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    override suspend fun getMetadata(): RepositoryMetadata = metadataResource.getFreshAndUpdateState()

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        try {
            withContext(ioDispatcher) {
                mutationMutex.withLock {
                    val oldMetadata = readMetadataFromDrive() ?: RepositoryMetadata()
                    val newMetadata = transform(oldMetadata)
                    val metadataFile = findMetadataFile()
                    val data = Json.encodeToString(newMetadata).toByteArray()
                    storage.uploadFile(
                        metadataFolderId,
                        ARCHIVE_METADATA_FILENAME,
                        ByteArrayInputStream(data),
                        data.size.toLong(),
                        metadataFile?.id,
                    )
                }
            }
        } finally {
            metadataLastChangeFlow.update { Date() }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readMetadataFromDrive(): RepositoryMetadata? {
        val metadataFile = findMetadataFile() ?: return null
        if (metadataFile.isGoogleAppDocument) {
            throw UnsupportedOperationException("Repository metadata must be a binary file")
        }

        return storage.downloadFile(metadataFile.id).use { Json.decodeFromStream(it) }
    }

    private fun findMetadataFile(): GoogleDriveFile? {
        val matches = storage.listChildren(metadataFolderId).filter { it.name == ARCHIVE_METADATA_FILENAME }
        if (matches.size > 1) {
            throw GoogleDriveAmbiguousFileException(ARCHIVE_METADATA_FILENAME)
        }
        return matches.singleOrNull()
    }

    private fun GoogleDriveFile.toArchiveFileInfo(): ArchiveFileInfo {
        if (isGoogleAppDocument || size == null || sha256Checksum == null) {
            throw UnsupportedOperationException("Google Drive file '$name' does not have binary size and SHA-256 metadata")
        }
        return ArchiveFileInfo(size, sha256Checksum)
    }

    private suspend fun initialize(createMetadataFolder: Boolean) {
        withContext(ioDispatcher) {
            val root = storage.getFile(rootFolderId)
            if (!root.isFolder) {
                throw IllegalArgumentException("Google Drive root '$rootFolderId' is not a folder")
            }

            val folders = storage.listChildren(rootFolderId).filter { it.name == METADATA_FOLDER_NAME }
            if (folders.size > 1) {
                throw GoogleDriveAmbiguousFileException(METADATA_FOLDER_NAME)
            }

            metadataFolderId =
                folders.singleOrNull()?.let { folder ->
                    if (!folder.isFolder) {
                        throw IllegalStateException("Google Drive item '$METADATA_FOLDER_NAME' is not a folder")
                    }
                    folder.id
                } ?: if (createMetadataFolder) {
                    storage.createFolder(rootFolderId, METADATA_FOLDER_NAME).id
                } else {
                    throw GoogleDriveLocationNotInitializedAsRepositoryException()
                }
        }
    }

    override suspend fun close() {
        scope.cancel()
    }

    companion object {
        private const val METADATA_FOLDER_NAME = ".ArchiveKeepMetadata"

        private suspend fun newRepository(
            storage: GoogleDriveStorage,
            rootFolderId: String,
            sharingDispatcher: CoroutineDispatcher,
            ioDispatcher: CoroutineDispatcher,
        ): GoogleDriveRepository =
            GoogleDriveRepository(storage, rootFolderId, sharingDispatcher, ioDispatcher).apply {
                initialize(createMetadataFolder = true)
                updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            }

        private suspend fun openRepository(
            storage: GoogleDriveStorage,
            rootFolderId: String,
            sharingDispatcher: CoroutineDispatcher,
            ioDispatcher: CoroutineDispatcher,
        ): GoogleDriveRepository =
            GoogleDriveRepository(storage, rootFolderId, sharingDispatcher, ioDispatcher).apply {
                initialize(createMetadataFolder = false)

                val metadata = readMetadataFromDrive() ?: throw GoogleDriveLocationNotInitializedAsRepositoryException()
                val repositoryType = metadata.repositoryType
                when (repositoryType) {
                    PLAIN_REPOSITORY_TYPE -> {}
                    null -> updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
                    else -> throw GoogleDriveUnsupportedRepositoryTypeException(repositoryType)
                }
            }

        suspend fun create(
            drive: Drive,
            rootFolderId: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): GoogleDriveRepository = newRepository(GoogleDriveStorage(drive), rootFolderId, sharingDispatcher, ioDispatcher)

        suspend fun open(
            drive: Drive,
            rootFolderId: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): GoogleDriveRepository = openRepository(GoogleDriveStorage(drive), rootFolderId, sharingDispatcher, ioDispatcher)
    }
}
