package com.sirktv.app.storage

import com.sirktv.app.domain.model.SavedCredentials

interface CredentialStore {
    suspend fun save(credentials: SavedCredentials)
    suspend fun get(): SavedCredentials?
    suspend fun clear()
}
