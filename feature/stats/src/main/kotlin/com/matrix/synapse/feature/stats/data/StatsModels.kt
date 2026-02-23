package com.matrix.synapse.feature.stats.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerVersionResponse(
    @SerialName("server_version") val serverVersion: String,
)

@Serializable
data class DatabaseRoomStatsResponse(
    val rooms: List<RoomSizeEntry> = emptyList(),
)

@Serializable
data class RoomSizeEntry(
    @SerialName("room_id") val roomId: String,
    @SerialName("estimated_size") val estimatedSize: Long = 0L,
)

@Serializable
data class MediaUsageResponse(
    val users: List<UserMediaStats> = emptyList(),
    @SerialName("next_token") val nextToken: Int? = null,
    val total: Int = 0,
)

@Serializable
data class UserMediaStats(
    @SerialName("user_id") val userId: String,
    val displayname: String? = null,
    @SerialName("media_count") val mediaCount: Int = 0,
    @SerialName("media_length") val mediaLength: Long = 0L,
)
