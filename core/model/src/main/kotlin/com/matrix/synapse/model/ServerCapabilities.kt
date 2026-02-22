package com.matrix.synapse.model

/**
 * Capability flags resolved from the Synapse server version.
 *
 * Suspend/unsuspend was added in Synapse 1.73.0.
 * Device management and media deletion have been available since early versions.
 */
data class ServerCapabilities(
    val synapseVersion: String,
    val canSuspendUsers: Boolean,
    val canManageDevices: Boolean,
    val canDeleteMedia: Boolean,
)
