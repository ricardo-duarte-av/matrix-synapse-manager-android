package com.matrix.synapse.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * Matrix Client-Server well-known discovery (/.well-known/matrix/client).
 * Served at https://&lt;server_name&gt;/.well-known/matrix/client per spec.
 * Used to resolve the server name for user IDs: if the response's base_url
 * points to our configured server URL, the host we requested is the server_name.
 */
@Serializable
data class MatrixClientWellKnown(
    @SerialName("m.homeserver") val homeserver: MHomeServer? = null,
)

@Serializable
data class MHomeServer(
    @SerialName("base_url") val baseUrl: String? = null,
)

interface MatrixWellKnownApi {
    @GET(".well-known/matrix/client")
    suspend fun getClient(): MatrixClientWellKnown
}
