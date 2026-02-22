package com.matrix.synapse.feature.devices.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    @SerialName("device_id") val deviceId: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("last_seen_ts") val lastSeenTs: Long? = null,
    @SerialName("last_seen_ip") val lastSeenIp: String? = null,
    @SerialName("user_id") val userId: String? = null,
)

@Serializable
data class DevicesListResponse(
    @SerialName("devices") val devices: List<DeviceInfo> = emptyList(),
    @SerialName("total") val total: Long = 0L,
)
