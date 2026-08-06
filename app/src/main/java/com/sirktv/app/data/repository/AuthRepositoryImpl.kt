package com.sirktv.app.data.repository

import com.sirktv.app.data.mapper.AuthResponseMapper
import com.sirktv.app.domain.model.LoginResult
import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.repository.AuthRepository
import com.sirktv.app.network.XtreamApiService
import com.sirktv.app.network.XtreamUrlBuilder
import com.sirktv.app.storage.CredentialStore
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val credentialStore: CredentialStore
) : AuthRepository {

    override suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        rememberMe: Boolean
    ): LoginResult {
        val playerApiUrl = try {
            XtreamUrlBuilder.buildPlayerApiUrl(serverUrl)
        } catch (e: IllegalArgumentException) {
            return LoginResult.InvalidServerUrl(e.message ?: "Invalid server address")
        }

        val response = try {
            apiService.authenticate(playerApiUrl, username, password)
        } catch (e: HttpException) {
            return if (e.code() in 400..499) {
                LoginResult.InvalidCredentials
            } else {
                LoginResult.ServerError("Server returned an error (${e.code()})")
            }
        } catch (e: SerializationException) {
            return LoginResult.ServerError("Unexpected response - is this a valid Xtream Codes server?")
        } catch (e: IOException) {
            return LoginResult.NetworkError(e.message ?: "Unable to reach the server")
        }

        val result = AuthResponseMapper.toLoginResult(response, serverUrl)
        if (result is LoginResult.Success && rememberMe) {
            credentialStore.save(SavedCredentials(serverUrl, username, password))
        }
        return result
    }

    override suspend fun getSavedCredentials(): SavedCredentials? = credentialStore.get()

    override suspend fun clearSavedCredentials() = credentialStore.clear()
}
