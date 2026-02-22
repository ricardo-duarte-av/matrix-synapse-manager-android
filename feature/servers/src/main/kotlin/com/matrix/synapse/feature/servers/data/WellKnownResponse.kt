package com.matrix.synapse.feature.servers.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WellKnownResponse(
    @SerialName("m.homeserver") val homeserver: HomeserverInfo? = null,
)

@Serializable
data class HomeserverInfo(
    @SerialName("base_url") val baseUrl: String,
)
