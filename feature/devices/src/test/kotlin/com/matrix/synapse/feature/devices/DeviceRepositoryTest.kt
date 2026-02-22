package com.matrix.synapse.feature.devices

import com.matrix.synapse.feature.devices.data.DeviceRepository
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

class DeviceRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DeviceRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val factory = RetrofitFactory(OkHttpClient(), Json { ignoreUnknownKeys = true })
        repository = DeviceRepository(factory)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun deletes_selected_device_and_refreshes_list() = runTest {
        // 1. Delete device (empty response)
        server.enqueue(MockResponse().setBody("{}"))
        // 2. Refresh list
        server.enqueue(
            MockResponse().setBody(
                """{"devices":[{"device_id":"REMAINING_DEVICE","display_name":"Phone"}],"total":1}"""
            )
        )

        val url = server.url("/").toString()
        repository.deleteDevice(url, "@alice:server", "OLD_DEVICE")
        val devices = repository.listDevices(url, "@alice:server")

        assertEquals(2, server.requestCount)
        assertEquals(1, devices.devices.size)
        assertEquals("REMAINING_DEVICE", devices.devices[0].deviceId)

        val deleteRequest = server.takeRequest()
        assertEquals("DELETE", deleteRequest.method)
        assertTrue(deleteRequest.path!!.contains("devices"))
    }

    @Test
    fun whois_response_maps_active_sessions() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "user_id": "@alice:server",
                  "devices": {
                    "DEVICE_A": {
                      "sessions": [
                        {
                          "connections": [
                            {"ip": "1.2.3.4", "last_seen": 1700000000000, "user_agent": "Element/1.0"}
                          ]
                        }
                      ]
                    }
                  }
                }
                """.trimIndent()
            )
        )

        val whois = repository.getWhois(server.url("/").toString(), "@alice:server")

        assertEquals("@alice:server", whois.userId)
        assertTrue("Expected at least one device in whois", whois.devices.isNotEmpty())
    }

    @Test
    fun list_devices_returns_all_devices_for_user() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"devices":[{"device_id":"DEV1","display_name":"Laptop"},{"device_id":"DEV2","display_name":"Phone"}],"total":2}"""
            )
        )

        val result = repository.listDevices(server.url("/").toString(), "@alice:server")

        assertEquals(2, result.devices.size)
        assertEquals("DEV1", result.devices[0].deviceId)
    }
}
