package com.matrix.synapse.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class ServerVersionResponse(
    @SerialName("server_version") val serverVersion: String,
    @SerialName("python_version") val pythonVersion: String? = null,
)

interface SynapseApi {
    @GET("/_synapse/admin/v1/server_version")
    suspend fun getServerVersion(): ServerVersionResponse
}
