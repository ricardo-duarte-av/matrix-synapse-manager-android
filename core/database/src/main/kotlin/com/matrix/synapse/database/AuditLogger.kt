package com.matrix.synapse.database

/**
 * Abstraction over the audit log storage backend.
 *
 * The Room-backed implementation is [RoomAuditLogger]. Unit tests use an in-memory fake.
 */
interface AuditLogger {
    suspend fun insert(entry: AuditLogEntry): Long
    suspend fun getAllByServer(serverId: String): List<AuditLogEntry>
    suspend fun deleteAll()
}
