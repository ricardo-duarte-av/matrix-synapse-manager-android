package com.matrix.synapse.feature.auth

import com.matrix.synapse.feature.auth.domain.SessionState
import com.matrix.synapse.feature.auth.domain.ValidateSessionUseCase
import com.matrix.synapse.model.Server
import com.matrix.synapse.network.RetrofitFactory
import com.matrix.synapse.security.InMemoryTokenStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ValidateSessionUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var useCase: ValidateSessionUseCase

    private val testServer = Server(
        id = "server1",
        displayName = "Test",
        inputUrl = "http://localhost",
        homeserverUrl = "http://localhost",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenStore = InMemoryTokenStore()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        useCase = ValidateSessionUseCase(factory, tokenStore)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun valid_token_returns_valid_state() = runTest {
        tokenStore.saveToken("server1", "good-token")
        server.enqueue(
            MockResponse().setBody(
                """{"server_version":"1.97.0","python_version":"3.11.0"}"""
            )
        )
        val serverWithUrl = testServer.copy(homeserverUrl = server.url("/").toString().trimEnd('/'))
        val state = useCase.validate(serverWithUrl)
        assertEquals(SessionState.Valid, state)
    }

    @Test
    fun expired_token_triggers_reauth_state() = runTest {
        tokenStore.saveToken("server1", "expired-token")
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"errcode":"M_UNKNOWN_TOKEN"}"""))
        val serverWithUrl = testServer.copy(homeserverUrl = server.url("/").toString().trimEnd('/'))
        val state = useCase.validate(serverWithUrl)
        assertEquals(SessionState.Expired, state)
    }

    @Test
    fun no_token_returns_no_token_state() = runTest {
        val state = useCase.validate(testServer)
        assertEquals(SessionState.NoToken, state)
    }
}
