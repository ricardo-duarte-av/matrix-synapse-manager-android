package com.matrix.synapse.feature.users

import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.network.RetrofitFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryPaginationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        repository = UserRepository(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun appends_next_page_when_next_token_present() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"users":[{"name":"@alice:server","displayname":"Alice"}],"next_token":"100","total":2}"""
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """{"users":[{"name":"@bob:server","displayname":"Bob"}],"total":2}"""
            )
        )

        val url = server.url("/").toString()
        val firstPage = repository.listUsers(url)
        val secondPage = repository.listUsers(url, from = firstPage.nextToken)

        val allUsers = firstPage.users + secondPage.users
        assertEquals(2, allUsers.size)
        assertEquals("@alice:server", allUsers[0].userId)
        assertEquals("@bob:server", allUsers[1].userId)
        assertNull(secondPage.nextToken)
    }

    @Test
    fun search_sends_name_query_parameter() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"users":[{"name":"@alice:server","displayname":"Alice"}],"total":1}"""
            )
        )

        val url = server.url("/").toString()
        val result = repository.listUsers(url, name = "alice")

        assertEquals(1, result.users.size)
        val request = server.takeRequest()
        assertTrue("Expected name query param in: ${request.path}", request.path!!.contains("name=alice"))
    }

    @Test
    fun empty_search_returns_all_users() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"users":[{"name":"@alice:server"},{"name":"@bob:server"}],"total":2}"""
            )
        )

        val result = repository.listUsers(server.url("/").toString())
        assertEquals(2, result.users.size)
    }
}
