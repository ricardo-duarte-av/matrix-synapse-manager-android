package com.matrix.synapse.feature.devices.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): DeviceAdminApi =
        retrofitFactory.create(serverUrl)

    suspend fun listDevices(serverUrl: String, userId: String): DevicesListResponse =
        api(serverUrl).listDevices(userId)

    suspend fun getDevice(serverUrl: String, userId: String, deviceId: String): DeviceInfo =
        api(serverUrl).getDevice(userId, deviceId)

    suspend fun deleteDevice(serverUrl: String, userId: String, deviceId: String) {
        api(serverUrl).deleteDevice(userId, deviceId)
    }

    suspend fun getWhois(serverUrl: String, userId: String): WhoisInfo =
        api(serverUrl).getWhois(userId)
}
