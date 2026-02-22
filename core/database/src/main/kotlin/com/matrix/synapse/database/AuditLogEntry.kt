package com.matrix.synapse.database

/**
 * Domain model for an audit log entry.
 * Used by [AuditLogger] interface and [ExportAuditLogUseCase].
 * Sensitive keys in [details] (access_token, password, token) are redacted on export.
 */
data class AuditLogEntry(
    val id: Long = 0L,
    val serverId: String,
    val action: AuditAction,
    val targetUserId: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap(),
)
