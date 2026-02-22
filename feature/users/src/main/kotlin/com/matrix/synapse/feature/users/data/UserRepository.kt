package com.matrix.synapse.feature.users.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): UserAdminApi =
        retrofitFactory.create(serverUrl)

    suspend fun listUsers(
        serverUrl: String,
        from: String? = null,
        limit: Int = 100,
        name: String? = null,
    ): UsersListResponse = api(serverUrl).listUsers(from = from, limit = limit, name = name)

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
