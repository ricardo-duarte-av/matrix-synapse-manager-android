package com.matrix.synapse.feature.moderation.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EventReportSummary(
    @SerialName("event_id") val eventId: String = "",
    val id: Long = 0L,
    val reason: String? = null,
    val score: Int? = null,
    @SerialName("received_ts") val receivedTs: Long = 0L,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("room_id") val roomId: String = "",
    val name: String? = null,
    val sender: String = "",
    @SerialName("user_id") val userId: String = "",
)

@Serializable
data class EventReportsListResponse(
    @SerialName("event_reports") val eventReports: List<EventReportSummary> = emptyList(),
    @SerialName("next_token") val nextToken: Long? = null,
    val total: Int = 0,
)

@Serializable
data class EventReportDetailResponse(
    @SerialName("event_id") val eventId: String = "",
    val id: Long = 0L,
    val reason: String? = null,
    val score: Int? = null,
    @SerialName("received_ts") val receivedTs: Long = 0L,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("room_id") val roomId: String = "",
    val name: String? = null,
    val sender: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("event_json") val eventJson: JsonElement? = null,
)
