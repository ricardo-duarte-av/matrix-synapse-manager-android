package com.matrix.synapse.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory implementation of [SecureTokenStore] for use in unit tests.
 * Not suitable for production — tokens are not encrypted and not persisted.
 */
class InMemoryTokenStore : SecureTokenStore {

    private val tokens = MutableStateFlow<Map<String, String>>(emptyMap())
    private val userIds = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun accessTokenFlow(serverId: String): Flow<String?> =
        tokens.map { it[serverId] }

    override suspend fun saveToken(serverId: String, accessToken: String) {
        tokens.value = tokens.value + (serverId to accessToken)
    }

    override suspend fun saveUserId(serverId: String, userId: String) {
        userIds.value = userIds.value + (serverId to userId)
    }

    override fun currentUserIdFlow(serverId: String): Flow<String?> =
        userIds.map { it[serverId] }

    override suspend fun clearTokens(serverId: String) {
        tokens.value = tokens.value - serverId
        userIds.value = userIds.value - serverId
    }
}
