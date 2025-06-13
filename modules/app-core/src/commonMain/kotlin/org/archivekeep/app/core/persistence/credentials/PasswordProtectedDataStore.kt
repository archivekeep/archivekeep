package org.archivekeep.app.core.persistence.credentials

interface PasswordProtectedDataStore<T> : ProtectedDataStore<T> {
    suspend fun create(password: String)

    suspend fun unlock(password: String)
}
