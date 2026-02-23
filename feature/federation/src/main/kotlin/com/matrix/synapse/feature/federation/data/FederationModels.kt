package com.matrix.synapse.feature.federation.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FederationDestination(
    val destination: String,
    @SerialName("retry_last_ts") val retryLastTs: Long = 0L,
    @SerialName("retry_interval") val retryInterval: Long = 0L,
    @SerialName("failure_ts") val failureTs: Long? = null,
    @SerialName("last_successful_stream_ordering") val lastSuccessfulStreamOrdering: Long? = null,
)

@Serializable
data class FederationDestinationsResponse(
    val destinations: List<FederationDestination> = emptyList(),
    val total: Int = 0,
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class DestinationRoom(
    @SerialName("room_id") val roomId: String,
    @SerialName("stream_ordering") val streamOrdering: Long = 0L,
)

@Serializable
data class DestinationRoomsResponse(
    val rooms: List<DestinationRoom> = emptyList(),
    val total: Int = 0,
    @SerialName("next_token") val nextToken: String? = null,
)
