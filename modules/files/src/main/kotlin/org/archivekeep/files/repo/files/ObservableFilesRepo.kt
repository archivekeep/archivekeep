package org.archivekeep.files.repo.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.files.repo.ObservableWorkingRepo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.coroutines.sharedResourceInGlobalScope
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DelicateCoroutinesApi::class, ExperimentalSerializationApi::class)
class ObservableFilesRepo internal constructor(
    val filesRepo: FilesRepo,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObservableWorkingRepo {
    val throttlePauseDuration: Duration = 500.milliseconds

    @OptIn(FlowPreview::class)
    private val calculationCause =
        filesRepo.root
            .watchRecursively(ioDispatcher)
            .debounce(100.milliseconds)
            .map { "update" }
            .sharedResourceInGlobalScope()
            .onStart { emit("start") }

    override val localIndex: Flow<StatusOperation.Result> =
        calculationCause
            .conflate()
            .transform {
                val result = StatusOperation(listOf("*")).execute(filesRepo)

                emit(result)

                println("Fetched local status: ${filesRepo.root}")

                // throttle
                delay(throttlePauseDuration)
            }.flowOn(Dispatchers.IO)
            .sharedResourceInGlobalScope()

    override val indexFlow =
        calculationCause
            .conflate()
            .transform {
                emit(filesRepo.index())

                println("Fetched index: ${filesRepo.root}")

                // throttle
                delay(throttlePauseDuration)
            }.flowOn(Dispatchers.IO)
            .mapToLoadable()
            .sharedResourceInGlobalScope()

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        calculationCause
            .conflate()
            .map {
                if (filesRepo.metadataPath.exists()) {
                    filesRepo.metadataPath.inputStream().use {
                        Loadable.Loaded(Json.decodeFromStream<RepositoryMetadata>(it)) as Loadable<RepositoryMetadata>
                    }
                } else {
                    if (filesRepo.archiveRoot.exists()) {
                        Loadable.Loaded(RepositoryMetadata())
                    } else {
                        throw RuntimeException("Something went wrong")
                    }
                }
            }.catch { e: Throwable ->
                emit(Loadable.Failed(e))
            }.transform {
                emit(it)

                println("Fetched metadata: ${filesRepo.root}")

                // throttle
                delay(throttlePauseDuration)
            }.catch { emit(Loadable.Failed(it)) }
            .flowOn(Dispatchers.IO)
            .sharedResourceInGlobalScope()
}
