package com.matrix.synapse.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthHeaderInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun attaches_bearer_token_when_provider_returns_token() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { "test-access-token" })
            .build()

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer test-access-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun omits_authorization_header_when_token_is_null() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor { null })
            .build()

        client.newCall(Request.Builder().url(server.url("/test")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }
}
