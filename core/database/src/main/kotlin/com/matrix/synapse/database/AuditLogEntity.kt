package com.matrix.synapse.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val serverId: String,
    val action: String,
    val targetUserId: String? = null,
    val timestampMs: Long,
    /** JSON-encoded details map; sensitive keys are already redacted before storage. */
    val detailsJson: String = "{}",
)

fun AuditLogEntry.toEntity(): AuditLogEntity = AuditLogEntity(
    id = id,
    serverId = serverId,
    action = action.name,
    targetUserId = targetUserId,
    timestampMs = timestampMs,
    detailsJson = buildString {
        append("{")
        details.entries.joinTo(this) { (k, v) -> "\"$k\":\"$v\"" }
        append("}")
    },
)

fun AuditLogEntity.toDomain(): AuditLogEntry = AuditLogEntry(
    id = id,
    serverId = serverId,
    action = runCatching { AuditAction.valueOf(action) }.getOrDefault(AuditAction.LOGIN),
    targetUserId = targetUserId,
    timestampMs = timestampMs,
)
