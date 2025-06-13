package org.archivekeep.app.core.utils.datastore

import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.InputStream
import java.io.OutputStream

class DataStoreKSerializer<T>(
    val serializer: KSerializer<T>,
    val defaultValueProducer: () -> T,
) : Serializer<T> {
    companion object {
        inline operator fun <reified T> invoke(noinline defaultValueProducer: () -> T): DataStoreKSerializer<T> =
            DataStoreKSerializer(
                Json.serializersModule.serializer(),
                defaultValueProducer,
            )
    }

    override val defaultValue: T
        get() = defaultValueProducer()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun readFrom(input: InputStream): T = Json.decodeFromStream(serializer, input)

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) {
        Json.encodeToStream(serializer, t, output)
    }
}
