package com.matrix.synapse.feature.users

import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
import com.matrix.synapse.network.RetrofitFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeactivateUserUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: DeactivateUserUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        val repository = UserRepository(factory)
        useCase = DeactivateUserUseCase(repository)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun deactivation_deletes_media_first_when_option_enabled() = runTest {
        // 1. Media list response
        server.enqueue(
            MockResponse().setBody(
                """{"media":[{"media_id":"abc123","media_length":1024},{"media_id":"def456","media_length":2048}],"total":2}"""
            )
        )
        // 2. Delete first media item
        server.enqueue(MockResponse().setBody("{}"))
        // 3. Delete second media item
        server.enqueue(MockResponse().setBody("{}"))
        // 4. Deactivate
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = true,
            confirmed = true,
        )

        assertTrue(result.isSuccess)
        assertEquals(4, server.requestCount)

        val listRequest = server.takeRequest()
        assertTrue("Should list user media first", listRequest.path!!.contains("media"))

        val delete1 = server.takeRequest()
        assertEquals("DELETE", delete1.method)

        val delete2 = server.takeRequest()
        assertEquals("DELETE", delete2.method)

        val deactivateRequest = server.takeRequest()
        assertEquals("POST", deactivateRequest.method)
        assertTrue("Path should be deactivate", deactivateRequest.path!!.contains("deactivate"))
        val body = deactivateRequest.body.readUtf8()
        assertTrue("erase should be true when media deleted: $body", body.contains("\"erase\":true"))
    }

    @Test
    fun typed_confirmation_is_required_for_deactivate() = runTest {
        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = false,
            confirmed = false,
        )

        assertTrue("Deactivation without confirmation should fail", result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun deactivation_continues_after_partial_media_delete_failure() = runTest {
        // Media list: two items
        server.enqueue(
            MockResponse().setBody("""{"media":[{"media_id":"abc123"},{"media_id":"def456"}],"total":2}""")
        )
        // First media delete fails (404)
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"errcode":"M_NOT_FOUND"}"""))
        // Second media delete succeeds
        server.enqueue(MockResponse().setBody("{}"))
        // Deactivate still proceeds
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = true,
            confirmed = true,
        )

        // Should succeed despite partial media failure
        assertTrue("Should succeed despite partial media delete failure", result.isSuccess)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun deactivation_without_media_cleanup_skips_media_endpoints() = runTest {
        server.enqueue(MockResponse().setBody("""{"id_server_unbind_result":"success"}"""))

        val result = useCase.deactivate(
            serverUrl = server.url("/").toString(),
            userId = "@alice:example.com",
            deleteMedia = false,
            confirmed = true,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("deactivate"))
    }
}
