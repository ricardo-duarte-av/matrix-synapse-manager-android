package com.matrix.synapse.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AuditLogEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auditLogDao(): AuditLogDao
}
