package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeactivateRequest(
    @SerialName("erase") val erase: Boolean = false,
)

@Serializable
data class DeactivateResponse(
    @SerialName("id_server_unbind_result") val idServerUnbindResult: String? = null,
)
