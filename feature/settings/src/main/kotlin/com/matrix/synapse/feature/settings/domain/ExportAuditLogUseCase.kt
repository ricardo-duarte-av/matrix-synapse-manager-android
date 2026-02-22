package com.matrix.synapse.feature.settings.domain

import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import javax.inject.Inject

/** Keys whose values must never appear in an exported audit log. */
private val SENSITIVE_KEYS = setOf("access_token", "token", "password", "refresh_token", "secret")

class ExportAuditLogUseCase @Inject constructor(
    private val auditLogger: AuditLogger,
) {
    /**
     * Returns a JSON array string of all audit log entries for [serverId].
     *
     * Values for known sensitive keys in the [AuditLogEntry.details] map are replaced
     * with `"[REDACTED]"` so tokens and passwords are never written to shared storage.
     */
    suspend fun exportAsJson(serverId: String): String {
        val entries = auditLogger.getAllByServer(serverId)
        if (entries.isEmpty()) return "[]"

        return buildString {
            append("[\n")
            entries.forEachIndexed { index, entry ->
                append("  ")
                append(entry.toJsonObject())
                if (index < entries.lastIndex) append(",")
                append("\n")
            }
            append("]")
        }
    }

    private fun AuditLogEntry.toJsonObject(): String = buildString {
        append("{")
        append("\"id\":$id,")
        append("\"serverId\":\"${serverId.escapeJson()}\",")
        append("\"action\":\"${action.name}\",")
        val uid = targetUserId
        if (uid != null) {
            append("\"targetUserId\":\"${uid.escapeJson()}\",")
        }
        append("\"timestampMs\":$timestampMs,")
        append("\"details\":{")
        val redactedDetails = details.entries.joinToString(",") { (k, v) ->
            val safeValue = if (k.lowercase() in SENSITIVE_KEYS) "[REDACTED]" else v.escapeJson()
            "\"${k.escapeJson()}\":\"$safeValue\""
        }
        append(redactedDetails)
        append("}}")
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}
