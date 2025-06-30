package org.archivekeep.utils.datastore.passwordprotected

interface PasswordProtectedDataStore<T> : ProtectedDataStore<T> {
    suspend fun create(password: String)

    suspend fun unlock(password: String)
}
