package org.archivekeep.app.android

import androidx.datastore.core.DataStoreFactory
import aws.smithy.kotlin.runtime.text.encoding.decodeBase64Bytes
import aws.smithy.kotlin.runtime.text.encoding.encodeBase64String
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.archivekeep.app.core.persistence.credentials.ProtectedDataStore
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.datastore.DataStoreKSerializer
import org.archivekeep.utils.coroutines.shareResourceIn
import java.nio.file.Path

class SecretKeyProtectedDataStore<E>(
    val file: Path,
    val serializer: KSerializer<E>,
    val defaultValueProducer: () -> E,
    val keyAlias: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : ProtectedDataStore<E> {
    @Serializable
    private data class DataAtRestStructure(
        val plaintextData: JsonElement?,
        val encryptedData: EncryptedData,
    ) {
        @Serializable
        data class EncryptedData(
            val iv: String,
            val contents: String,
        )
    }

    private val rawDataStore =
        DataStoreFactory.create(
            serializer =
                DataStoreKSerializer(
                    defaultValueProducer = { DataAtRestStructure(null, defaultValueProducer().encrypt()) },
                ),
            produceFile = { file.toFile() },
        )

    override suspend fun needsUnlock(): Boolean = false

    override val data: Flow<ProtectedLoadableResource<E, Any>> =
        rawDataStore
            .data
            .map {
                try {
                    ProtectedLoadableResource.Loaded(it.encryptedData.decrypt())
                } catch (e: Throwable) {
                    // TODO: don't purge automatically
                    rawDataStore.updateData {
                        DataAtRestStructure(null, defaultValueProducer().encrypt())
                    }

                    ProtectedLoadableResource.Failed(e)
                }
            }.shareResourceIn(scope)

    private fun DataAtRestStructure.EncryptedData.decrypt(): E {
        val decryptedText = SecurityUtil.decryptData(keyAlias, this.iv.decodeBase64Bytes(), this.contents.decodeBase64Bytes())

        return Json.decodeFromString(serializer, decryptedText)
    }

    private fun E.encrypt(): DataAtRestStructure.EncryptedData {
        val (iv, data) = SecurityUtil.encryptData(keyAlias, Json.encodeToString(serializer, this))

        return DataAtRestStructure.EncryptedData(iv.encodeBase64String(), data.encodeBase64String())
    }

    override suspend fun updateData(transform: suspend (t: E) -> E): E =
        rawDataStore
            .updateData { rawData ->
                rawData.copy(
                    encryptedData = transform(rawData.encryptedData.decrypt()).encrypt(),
                )
            }.encryptedData
            .decrypt()
}
