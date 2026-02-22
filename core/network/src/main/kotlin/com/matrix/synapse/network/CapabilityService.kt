package com.matrix.synapse.network

import com.matrix.synapse.model.ServerCapabilities
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves [ServerCapabilities] for a given Synapse server.
 *
 * Capabilities are derived from the Synapse version string (avoids intrusive
 * preflight probes on the user's behalf). Results are cached per [serverId]
 * so repeated calls are free.
 *
 * Suspend/unsuspend: added in Synapse 1.73.0.
 */
@Singleton
class CapabilityService @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private val cache = ConcurrentHashMap<String, ServerCapabilities>()

    suspend fun getCapabilities(serverId: String, serverUrl: String): ServerCapabilities =
        cache.getOrPut(serverId) {
            val api = retrofitFactory.create<SynapseApi>(serverUrl)
            val version = api.getServerVersion().serverVersion
            buildCapabilities(version)
        }

    fun invalidate(serverId: String) {
        cache.remove(serverId)
    }

    private fun buildCapabilities(versionStr: String): ServerCapabilities {
        val parsed = parseVersion(versionStr)
        return ServerCapabilities(
            synapseVersion = versionStr,
            canSuspendUsers = parsed >= SynapseVersion(1, 73, 0),
            canManageDevices = true,
            canDeleteMedia = true,
        )
    }

    private fun parseVersion(versionStr: String): SynapseVersion {
        val parts = versionStr.split(".").map { it.toIntOrNull() ?: 0 }
        return SynapseVersion(
            major = parts.getOrElse(0) { 0 },
            minor = parts.getOrElse(1) { 0 },
            patch = parts.getOrElse(2) { 0 },
        )
    }
}

private data class SynapseVersion(val major: Int, val minor: Int, val patch: Int) :
    Comparable<SynapseVersion> {
    override fun compareTo(other: SynapseVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }
}
