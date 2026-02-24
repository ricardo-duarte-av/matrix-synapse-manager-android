package com.matrix.synapse.feature.users.data

import com.matrix.synapse.network.MatrixWellKnownApi
import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): UserAdminApi =
        retrofitFactory.create(serverUrl)

    /**
     * Resolves the server name for Matrix IDs using .well-known/matrix/client.
     * Per spec, that file is served at https://&lt;server_name&gt;/.well-known/matrix/client.
     * We try the configured host and (if it starts with "matrix.") the parent domain;
     * if the response's m.homeserver.base_url points to our [serverUrl], that host is the server_name.
     * Returns null if well-known is not available or doesn't match.
     */
    suspend fun getServerNameFromWellKnown(serverUrl: String): String? {
        val ourHost = hostOf(serverUrl) ?: return null
        val candidates = if (ourHost.startsWith("matrix.")) {
            listOf(ourHost.removePrefix("matrix."), ourHost)
        } else {
            listOf(ourHost)
        }
        for (candidate in candidates.distinct()) {
            val base = "https://$candidate"
            val wellKnown = runCatching {
                retrofitFactory.create<MatrixWellKnownApi>(base).getClient()
            }.getOrNull() ?: continue
            val baseUrl = wellKnown.homeserver?.baseUrl?.trim()?.trimEnd('/') ?: continue
            val discoveredHost = hostOf(baseUrl) ?: continue
            if (discoveredHost.equals(ourHost, ignoreCase = true)) return candidate
        }
        return null
    }

    private fun hostOf(url: String): String? = runCatching {
        java.net.URI(url).host
    }.getOrNull()

    suspend fun listUsers(
        serverUrl: String,
        from: String? = null,
        limit: Int = 100,
        name: String? = null,
    ): UsersListResponse = api(serverUrl).listUsers(from = from, limit = limit, name = name)

    /**
     * Resolves the server name used in Matrix IDs (@user:serverName) from the server.
     * Calls the admin API (list users, limit 1) and parses server name from the first user's userId.
     * Returns null if there are no users yet (e.g. brand-new server); caller should use URL-based fallback.
     */
    suspend fun getServerNameFromApi(serverUrl: String): String? {
        val response = api(serverUrl).listUsers(limit = 1)
        val userId = response.users.firstOrNull()?.userId ?: return null
        if (!userId.startsWith("@") || !userId.contains(":")) return null
        return userId.substringAfter(':')
    }

    suspend fun getUser(serverUrl: String, userId: String): UserDetail =
        api(serverUrl).getUser(userId)

    suspend fun upsertUser(
        serverUrl: String,
        userId: String,
        request: UpsertUserRequest,
    ): UserDetail = api(serverUrl).upsertUser(userId, request)

    suspend fun setLocked(serverUrl: String, userId: String, locked: Boolean) {
        api(serverUrl).upsertUser(userId, UpsertUserRequest(locked = locked))
    }

    suspend fun setSuspended(serverUrl: String, userId: String, suspended: Boolean) {
        api(serverUrl).setSuspended(userId, SuspendRequest(suspend = suspended))
    }

    suspend fun deactivateUser(
        serverUrl: String,
        userId: String,
        erase: Boolean,
    ): DeactivateResponse = api(serverUrl).deactivateUser(userId, DeactivateRequest(erase = erase))

    suspend fun listUserMedia(serverUrl: String, userId: String): UserMediaListResponse =
        api(serverUrl).listUserMedia(userId)

    suspend fun deleteMedia(serverUrl: String, serverName: String, mediaId: String) {
        api(serverUrl).deleteMedia(serverName, mediaId)
    }
}
