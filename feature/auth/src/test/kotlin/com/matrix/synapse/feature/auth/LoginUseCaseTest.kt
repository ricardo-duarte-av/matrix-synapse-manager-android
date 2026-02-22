package com.matrix.synapse.feature.auth

import com.matrix.synapse.feature.auth.domain.LoginUseCase
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.InMemoryTokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var useCase: LoginUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = InMemoryTokenStore()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        useCase = LoginUseCase(factory, tokenStore)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun login_stores_access_token_on_success() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"tok_123","user_id":"@admin:example.com","device_id":"DEV1"}"""
            )
        )
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "server1",
            username = "admin",
            password = "secret",
        )
        assertNotNull("Login should succeed", result.getOrNull())
        assertEquals("tok_123", tokenStore.accessTokenFlow("server1").first())
    }

    @Test
    fun login_does_not_persist_password() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"tok_123","user_id":"@admin:example.com","device_id":"DEV1"}"""
            )
        )
        useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "server1",
            username = "admin",
            password = "super_secret",
        )
        // No method on SecureTokenStore returns a password — this is enforced at the type level.
        // Verify the stored value is the token, not the password.
        val stored = tokenStore.accessTokenFlow("server1").first()
        assertEquals("tok_123", stored)
        assert(stored != "super_secret") { "Password must never be stored" }
    }

    @Test
    fun login_returns_failure_on_forbidden() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(403)
                .setBody("""{"errcode":"M_FORBIDDEN","error":"Invalid password"}""")
        )
        val result = useCase.login(
            serverUrl = server.url("/").toString(),
            serverId = "server1",
            username = "admin",
            password = "wrong",
        )
        assert(result.isFailure) { "Expected failure on 403" }
        assertNull(tokenStore.accessTokenFlow("server1").first())
    }
}
