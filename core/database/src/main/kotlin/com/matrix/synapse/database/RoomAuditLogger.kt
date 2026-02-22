package com.matrix.synapse.database

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAuditLogger @Inject constructor(
    private val dao: AuditLogDao,
) : AuditLogger {

    override suspend fun insert(entry: AuditLogEntry): Long =
        dao.insert(entry.toEntity())

    override suspend fun getAllByServer(serverId: String): List<AuditLogEntry> =
        dao.getAllByServer(serverId).map { it.toDomain() }

    override suspend fun deleteAll() = dao.deleteAll()
}
