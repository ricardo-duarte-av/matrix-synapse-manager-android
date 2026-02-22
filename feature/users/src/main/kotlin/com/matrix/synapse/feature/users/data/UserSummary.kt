package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSummary(
    @SerialName("name") val userId: String,
    @SerialName("displayname") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_guest") val isGuest: Boolean = false,
    @SerialName("deactivated") val deactivated: Boolean = false,
    @SerialName("shadow_banned") val shadowBanned: Boolean = false,
    @SerialName("creation_ts") val creationTs: Long = 0L,
    @SerialName("locked") val locked: Boolean = false,
)
