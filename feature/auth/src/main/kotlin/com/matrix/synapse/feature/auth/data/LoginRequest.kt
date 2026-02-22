package com.matrix.synapse.feature.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("type") val type: String = "m.login.password",
    @SerialName("identifier") val identifier: UserIdentifier,
    @SerialName("password") val password: String,
)

@Serializable
data class UserIdentifier(
    @SerialName("type") val type: String = "m.id.user",
    @SerialName("user") val user: String,
)
