package com.matrix.synapse.feature.auth.domain

import com.matrix.synapse.model.Server
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.network.SynapseApi
import com.matrix.synapse.security.SecureTokenStore
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface SessionState {
    data object Valid : SessionState
    data object NoToken : SessionState
    data object Expired : SessionState
    data class Error(val cause: Throwable) : SessionState
}

class ValidateSessionUseCase @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: SecureTokenStore,
) {
    /**
     * Checks whether the stored token for [server] is still valid.
     * Calls the server version endpoint (low-impact read that requires admin auth).
     */
    suspend fun validate(server: Server): SessionState {
        val token = tokenStore.accessTokenFlow(server.id).first()
            ?: return SessionState.NoToken

        return try {
            val api = retrofitFactory.create<SynapseApi>(server.homeserverUrl)
            api.getServerVersion()
            SessionState.Valid
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) SessionState.Expired
            else SessionState.Error(e)
        } catch (e: IOException) {
            SessionState.Error(e)
        }
    }
}
