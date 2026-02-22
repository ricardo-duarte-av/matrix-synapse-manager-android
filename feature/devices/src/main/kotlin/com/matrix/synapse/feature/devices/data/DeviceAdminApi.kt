package com.matrix.synapse.feature.devices.data

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface DeviceAdminApi {

    @GET("/_synapse/admin/v2/users/{userId}/devices")
    suspend fun listDevices(@Path("userId") userId: String): DevicesListResponse

    @GET("/_synapse/admin/v2/users/{userId}/devices/{deviceId}")
    suspend fun getDevice(
        @Path("userId") userId: String,
        @Path("deviceId") deviceId: String,
    ): DeviceInfo

    @DELETE("/_synapse/admin/v2/users/{userId}/devices/{deviceId}")
    suspend fun deleteDevice(
        @Path("userId") userId: String,
        @Path("deviceId") deviceId: String,
    ): DevicesListResponse  // Synapse returns {} — DeactivateResponse reuse possible but DevicesListResponse with ignoreUnknownKeys absorbs it

    @GET("/_synapse/admin/v1/whois/{userId}")
    suspend fun getWhois(@Path("userId") userId: String): WhoisInfo
}
