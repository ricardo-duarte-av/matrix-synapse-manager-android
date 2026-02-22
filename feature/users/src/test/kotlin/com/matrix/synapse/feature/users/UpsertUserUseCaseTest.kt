package com.matrix.synapse.feature.users

import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.UpsertUserUseCase
import com.matrix.synapse.network.RetrofitFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpsertUserUseCaseTest {

    private lateinit var server: MockWebServer
    private lateinit var useCase: UpsertUserUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        val repository = UserRepository(factory)
        useCase = UpsertUserUseCase(repository)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun creates_new_user_with_required_fields() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"name":"@newuser:server","displayname":"New User","admin":false}"""
            )
        )

        val result = useCase.createUser(
            serverUrl = server.url("/").toString(),
            userId = "@newuser:server",
            password = "s3cureP@ss",
            displayName = "New User",
            admin = false,
        )

        assertTrue(result.isSuccess)
        assertEquals("@newuser:server", result.getOrThrow().userId)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        val body = request.body.readUtf8()
        assertTrue("Body should contain password: $body", body.contains("\"password\""))
        assertTrue("Body should contain displayname: $body", body.contains("\"displayname\""))
    }

    @Test
    fun updates_existing_user_without_overwriting_unset_fields() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"name":"@alice:server","displayname":"Alice Updated","admin":false}"""
            )
        )

        val result = useCase.updateUser(
            serverUrl = server.url("/").toString(),
            userId = "@alice:server",
            displayName = "Alice Updated",
            admin = null,
        )

        assertTrue(result.isSuccess)

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue("Body should contain displayname: $body", body.contains("\"displayname\""))
        assertFalse("Body must NOT contain password: $body", body.contains("\"password\""))
        assertFalse("Body must NOT contain admin (null): $body", body.contains("\"admin\""))
    }

    @Test
    fun create_user_fails_with_invalid_user_id_format() = runTest {
        val result = useCase.createUser(
            serverUrl = server.url("/").toString(),
            userId = "notavaliduserid",
            password = "s3cureP@ss",
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun create_user_fails_when_password_too_short() = runTest {
        val result = useCase.createUser(
            serverUrl = server.url("/").toString(),
            userId = "@user:server",
            password = "short",
        )

        assertTrue(result.isFailure)
    }
}
