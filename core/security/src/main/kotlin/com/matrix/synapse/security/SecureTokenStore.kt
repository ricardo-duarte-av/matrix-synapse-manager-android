package com.matrix.synapse.security

import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting per-server access tokens.
 *
 * Scope: access tokens only. Passwords are NEVER persisted — callers
 * must not pass raw credentials here. There is intentionally no
 * savePassword method on this interface.
 */
interface SecureTokenStore {
    /** Emits the current access token for [serverId], or null if none stored. */
    fun accessTokenFlow(serverId: String): Flow<String?>

    /** Persists the [accessToken] for [serverId], overwriting any existing value. */
    suspend fun saveToken(serverId: String, accessToken: String)

    /** Removes all stored tokens for [serverId]. */
    suspend fun clearTokens(serverId: String)
}
