package org.archivekeep.app.core.persistence.credentials

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.PasswordBasedDecrypter
import com.nimbusds.jose.crypto.PasswordBasedEncrypter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.generics.sharedWhileSubscribed
import org.archivekeep.utils.safeFileRead
import org.archivekeep.utils.safeFileReadWrite
import java.nio.file.Path

/**
 * It can be in three states:
 *
 * - empty - file doesn't exist
 * - locked - file exists, but not unlocked
 * - unlocked - file exists, and unlocked
 *
 * Other unhappy paths:
 *
 * - unlocked but errored - password changed by other process, or other I/O error
 */
class JoseStorage<T>(
    val file: Path,
    val serializer: KSerializer<T>,
    val defaultValueProducer: () -> T,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : ProtectedDataStore<T> {
    private val mutex = Mutex()

    private val currentStateFlow = MutableStateFlow<State<T>>(State.NotInitialized)

    val autoloadFlow =
        currentStateFlow
            .onStart {
                tryInitialize()
            }.stateIn(scope, SharingStarted.Lazily, State.NotInitialized)

    override val data =
        autoloadFlow
            .map {
                when (it) {
                    is State.NotInitialized ->
                        ProtectedLoadableResource.Loading
                    is State.NotExisting ->
                        ProtectedLoadableResource.Loaded(it.defaultData)
                    is State.Locked ->
                        ProtectedLoadableResource.PendingAuthentication("TODO")
                    is State.Unlocked ->
                        ProtectedLoadableResource.Loaded(it.data)
                }
            }.sharedWhileSubscribed(scope)

    suspend fun tryInitialize() {
        mutex.withLock {
            if (currentStateFlow.value is State.NotInitialized) {
                val contents = safeFileRead(file)

                if (contents == null) {
                    currentStateFlow.value =
                        State.NotExisting(
                            defaultData = defaultValueProducer(),
                        )
                } else {
                    currentStateFlow.value = State.Locked
                }
            }
        }
    }

    suspend fun create(password: String) {
        mutex.withLock {
            var newCreated: T? = null

            safeFileReadWrite(file) { old ->
                if (old != null) {
                    throw RuntimeException("Already exists")
                }

                val new = defaultValueProducer()
                newCreated = new

                encrypt(Json.encodeToString(serializer, new), password)
            }

            val new = newCreated!!

            currentStateFlow.value = State.Unlocked(password, new)
        }
    }

    suspend fun unlock(password: String) {
        mutex.withLock {
            val contents = safeFileRead(file) ?: throw RuntimeException("File doesn't exist")

            val data = decryptDecode(contents, password)

            currentStateFlow.value = State.Unlocked(password, data)
        }
    }

    private fun decryptDecode(
        contents: String,
        password: String,
    ): T = Json.decodeFromString(serializer, decrypt(contents, password))

    override suspend fun updateData(transform: suspend (t: T) -> T): T =
        mutex.withLock {
            val v = currentStateFlow.value

            if (v is State.Unlocked) {
                var newCreated: T? = null

                safeFileReadWrite(
                    file,
                    transform = { oldString ->
                        val old =
                            oldString?.let {
                                decryptDecode(it, password = v.password)
                            } ?: defaultValueProducer()

                        val new = transform(old)

                        newCreated = new

                        encrypt(Json.encodeToString(serializer, new), v.password)
                    },
                )

                val new = newCreated!!

                currentStateFlow.value = State.Unlocked(v.password, new)

                new
            } else {
                // TODO: out of order could work
                throw RuntimeException("Not unlocked")
            }
        }

    fun decrypt(
        cipherText: String,
        pass: String,
    ): String {
        val jweObject = JWEObject.parse(cipherText)

        jweObject.decrypt(PasswordBasedDecrypter(pass))

        return jweObject.payload.toString()
    }

    fun encrypt(
        rawString: String,
        pass: String,
    ): String {
        val header = JWEHeader(JWEAlgorithm.PBES2_HS512_A256KW, EncryptionMethod.A128GCM)

        val jweObject = JWEObject(header, Payload(rawString))
        jweObject.encrypt(PasswordBasedEncrypter(pass, 40, 16000))

        return jweObject.serialize()
    }

    sealed interface State<out T> {
        data class NotExisting<out T>(
            val defaultData: T,
        ) : State<T>

        data class Unlocked<out T>(
            val password: String,
            val data: T,
        ) : State<T>

        data object NotInitialized : State<Nothing>

        data object Locked : State<Nothing>
    }
}
