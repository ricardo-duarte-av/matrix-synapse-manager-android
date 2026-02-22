package com.matrix.synapse.security

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SecureTokenStore].
 * Uses [InMemoryTokenStore] (a test-only fake) to avoid Android Keystore dependency.
 */
class TokenStoreTest {

    private lateinit var store: SecureTokenStore

    @Before
    fun setUp() {
        store = InMemoryTokenStore()
    }

    @Test
    fun saves_and_reads_access_token_by_server_id() = runTest {
        store.saveToken(serverId = "server1", accessToken = "token-abc")
        assertEquals("token-abc", store.accessTokenFlow("server1").first())
    }

    @Test
    fun returns_null_for_unknown_server_id() = runTest {
        assertNull(store.accessTokenFlow("unknown").first())
    }

    @Test
    fun clears_tokens_for_server_id() = runTest {
        store.saveToken(serverId = "server1", accessToken = "token-abc")
        store.clearTokens(serverId = "server1")
        assertNull(store.accessTokenFlow("server1").first())
    }

    @Test
    fun different_server_ids_are_isolated() = runTest {
        store.saveToken(serverId = "server1", accessToken = "token-1")
        store.saveToken(serverId = "server2", accessToken = "token-2")
        assertEquals("token-1", store.accessTokenFlow("server1").first())
        assertEquals("token-2", store.accessTokenFlow("server2").first())
    }

    @Test
    fun no_password_method_exists_on_interface() {
        // The interface must not expose any method accepting a "password" parameter.
        // This is enforced at the type level: SecureTokenStore has no savePassword method.
        val methods = SecureTokenStore::class.java.methods.map { it.name }
        assert(methods.none { it.contains("password", ignoreCase = true) }) {
            "SecureTokenStore must not expose password-related methods. Found: $methods"
        }
    }
}
