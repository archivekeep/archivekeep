package org.archivekeep.app.core.persistence.drivers.grpc

import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.core.utils.identifiers.TypedRepoURIData

data class GRPCRepositoryURIData(
    val hostname: String,
    val port: Short,
    val resourceName: String,
) : TypedRepoURIData {
    override val storageURI = StorageURI("grpc", "//$hostname:$port")

    override val defaultLabel = resourceName

    fun serialized(): String = "//$hostname:$port/$resourceName"

    companion object {
        fun fromSerialized(rawString: String): GRPCRepositoryURIData {
            val parts = rawString.trimStart('/').split("/", limit = 2)

            if (parts.size != 2) {
                throw RuntimeException("Wrong repository ID: $rawString")
            }

            val (hostnamePort, resourceName) = parts
            val (hostname, port) = hostnamePort.split(":", limit = 2)

            return GRPCRepositoryURIData(
                hostname = hostname,
                port = port.toShort(),
                resourceName = resourceName,
            )
        }
    }
}
