package com.matrix.synapse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AuditLogEntity): Long

    @Query("SELECT * FROM audit_log WHERE serverId = :serverId ORDER BY timestampMs DESC")
    suspend fun getAllByServer(serverId: String): List<AuditLogEntity>

    @Query("DELETE FROM audit_log")
    suspend fun deleteAll()
}
