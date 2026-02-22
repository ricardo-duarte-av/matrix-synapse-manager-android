package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsersListResponse(
    @SerialName("users") val users: List<UserSummary>,
    @SerialName("next_token") val nextToken: String? = null,
    @SerialName("total") val total: Long = 0L,
)
