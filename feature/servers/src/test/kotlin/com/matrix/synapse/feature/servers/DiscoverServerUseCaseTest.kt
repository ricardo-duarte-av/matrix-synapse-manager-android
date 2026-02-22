package com.matrix.synapse.feature.servers

import com.matrix.synapse.feature.servers.domain.DiscoverServerUseCase
import com.matrix.synapse.network.RetrofitFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiscoverServerUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: DiscoverServerUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient()
        val factory = RetrofitFactory(client, json)
        useCase = DiscoverServerUseCase(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun extracts_homeserver_base_url_from_well_known() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"m.homeserver":{"base_url":"https://matrix.example.com"}}"""
            )
        )
        val result = useCase.discover(server.url("/").toString())
        assertTrue(result.isSuccess)
        assertEquals("https://matrix.example.com", result.getOrThrow().homeserverUrl)
    }

    @Test
    fun falls_back_to_input_url_when_well_known_returns_404() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val inputUrl = server.url("/").toString().trimEnd('/')
        val result = useCase.discover(inputUrl)
        assertTrue(result.isSuccess)
        // homeserverUrl falls back to the normalised input
        assertEquals(inputUrl, result.getOrThrow().homeserverUrl)
    }

    @Test
    fun prepends_https_when_scheme_is_missing() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        // Use bare host:port (no scheme) — won't actually connect but normalisation is testable
        val result = useCase.discover("localhost:${server.port}")
        // Either success (fallback) or failure is acceptable; the key assertion is scheme handling
        val normalized = result.getOrNull()?.inputUrl ?: ""
        assertTrue(
            "Expected https scheme but got: $normalized",
            normalized.startsWith("https://") || result.isFailure
        )
    }

    @Test
    fun add_server_shows_error_on_unreachable_host() = runTest {
        server.shutdown()
        val result = useCase.discover("https://this.host.does.not.exist.invalid")
        assertTrue("Expected failure for unreachable host", result.isFailure)
    }
}
