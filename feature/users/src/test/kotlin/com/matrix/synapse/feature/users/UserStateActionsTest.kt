package com.matrix.synapse.feature.users

import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.network.CapabilityService
import com.matrix.synapse.network.RetrofitFactory
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

class UserStateActionsTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UserRepository
    private lateinit var capabilityService: CapabilityService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        repository = UserRepository(factory)
        capabilityService = CapabilityService(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun toggles_locked_flag_via_user_put() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"name":"@alice:server","locked":true,"deactivated":false}"""
            )
        )

        repository.setLocked(server.url("/").toString(), "@alice:server", true)

        val request = server.takeRequest()
        assertEquals("PUT", request.method!!)
        assertTrue(
            "Path should target user: ${request.path}",
            request.path!!.contains("%40alice%3Aserver") || request.path!!.contains("@alice:server"),
        )
        val body = request.body.readUtf8()
        assertTrue("Body should contain locked:true: $body", body.contains("\"locked\":true"))
    }

    @Test
    fun hides_suspend_action_when_capability_missing() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"server_version":"1.72.0","python_version":"3.10.0"}"""
            )
        )

        val caps = capabilityService.getCapabilities("server1", server.url("/").toString())

        assertFalse("Expected suspend unsupported for 1.72.0", caps.canSuspendUsers)
    }

    @Test
    fun suspend_action_available_when_capability_present() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"server_version":"1.73.0","python_version":"3.11.0"}"""
            )
        )

        val caps = capabilityService.getCapabilities("server2", server.url("/").toString())

        assertTrue("Expected suspend supported for 1.73.0", caps.canSuspendUsers)
    }

    @Test
    fun set_suspended_sends_suspend_put_request() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        repository.setSuspended(server.url("/").toString(), "@alice:server", true)

        val request = server.takeRequest()
        assertEquals("PUT", request.method!!)
        assertTrue(
            "Path should be suspend endpoint: ${request.path}",
            request.path!!.contains("suspend"),
        )
        val body = request.body.readUtf8()
        assertTrue("Body should contain suspend:true: $body", body.contains("\"suspend\":true"))
    }
}

// Local assertion alias so we don't need to import Assert.assertEquals each time
private fun assertEquals(expected: String, actual: String) {
    org.junit.Assert.assertEquals(expected, actual)
}
