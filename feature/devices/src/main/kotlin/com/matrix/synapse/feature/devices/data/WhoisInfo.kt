package com.matrix.synapse.feature.devices.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from GET /_synapse/admin/v1/whois/{userId}.
 *
 * The API returns a map of deviceId → DeviceActivity so we use a generic map and
 * flatten it into a list for easier UI consumption.
 */
@Serializable
data class WhoisInfo(
    @SerialName("user_id") val userId: String,
    @SerialName("devices") val devices: Map<String, WhoisDeviceActivity> = emptyMap(),
)

@Serializable
data class WhoisDeviceActivity(
    @SerialName("sessions") val sessions: List<WhoisSession> = emptyList(),
)

@Serializable
data class WhoisSession(
    @SerialName("connections") val connections: List<WhoisConnection> = emptyList(),
)

@Serializable
data class WhoisConnection(
    @SerialName("ip") val ip: String? = null,
    @SerialName("last_seen") val lastSeen: Long? = null,
    @SerialName("user_agent") val userAgent: String? = null,
)
