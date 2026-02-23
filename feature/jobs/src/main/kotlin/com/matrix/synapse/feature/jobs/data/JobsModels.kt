package com.matrix.synapse.feature.jobs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundUpdatesStatusResponse(
    val enabled: Boolean = true,
    @SerialName("current_updates") val currentUpdates: Map<String, CurrentUpdateInfo> = emptyMap(),
)

@Serializable
data class CurrentUpdateInfo(
    val name: String = "",
    @SerialName("total_item_count") val totalItemCount: Long = 0L,
    @SerialName("total_duration_ms") val totalDurationMs: Double = 0.0,
    @SerialName("average_items_per_ms") val averageItemsPerMs: Double = 0.0,
)

@Serializable
data class EnabledResponse(val enabled: Boolean)

@Serializable
data class EnabledRequest(val enabled: Boolean)

@Serializable
data class StartJobRequest(@SerialName("job_name") val jobName: String)
