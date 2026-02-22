package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMediaListResponse(
    @SerialName("media") val media: List<MediaItem> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
    @SerialName("total") val total: Long = 0L,
)

@Serializable
data class MediaItem(
    @SerialName("media_id") val mediaId: String,
    @SerialName("created_ts") val createdTs: Long = 0L,
    @SerialName("upload_name") val uploadName: String? = null,
    @SerialName("media_length") val mediaLength: Long = 0L,
)
