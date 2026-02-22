package com.matrix.synapse.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CapabilityServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: CapabilityService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        service = CapabilityService(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun marks_suspend_supported_when_version_is_1_73_or_newer() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"server_version":"1.97.0","python_version":"3.11.0"}""")
        )
        val caps = service.getCapabilities(
            serverId = "server1",
            serverUrl = server.url("/").toString(),
        )
        assertTrue("Expected suspend supported for 1.97.0", caps.canSuspendUsers)
    }

    @Test
    fun marks_suspend_unsupported_when_endpoint_returns_unrecognized_old_version() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"server_version":"1.72.0","python_version":"3.10.0"}""")
        )
        val caps = service.getCapabilities(
            serverId = "server2",
            serverUrl = server.url("/").toString(),
        )
        assertFalse("Expected suspend unsupported for 1.72.0", caps.canSuspendUsers)
    }

    @Test
    fun capabilities_are_cached_per_server_id() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"server_version":"1.80.0","python_version":"3.11.0"}""")
        )
        val url = server.url("/").toString()
        val first = service.getCapabilities("cached_server", url)
        // Second call should NOT hit the network (server has no more queued responses)
        val second = service.getCapabilities("cached_server", url)
        assertTrue(first == second)
        // Only one request should have been made
        assert(server.requestCount == 1) { "Expected 1 request but got ${server.requestCount}" }
    }
}
