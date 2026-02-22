package com.matrix.synapse.feature.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String? = null,
)
