package com.matrix.synapse.security

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting per-server access tokens and current user identity.
 *
 * Scope: access tokens and user ID only. Passwords are NEVER persisted — callers
 * must not pass raw credentials here. There is intentionally no
 * savePassword method on this interface.
 */
interface SecureTokenStore {
    /** Emits the current access token for [serverId], or null if none stored. */
    fun accessTokenFlow(serverId: String): Flow<String?>

    /** Persists the [accessToken] for [serverId], overwriting any existing value. */
    suspend fun saveToken(serverId: String, accessToken: String)

    /** Persists the logged-in [userId] for [serverId]. Call after successful login. */
    suspend fun saveUserId(serverId: String, userId: String)

    /** Emits the current user ID for [serverId], or null if none stored. */
    fun currentUserIdFlow(serverId: String): Flow<String?>

    /** Removes all stored tokens and user ID for [serverId]. */
    suspend fun clearTokens(serverId: String)
}
