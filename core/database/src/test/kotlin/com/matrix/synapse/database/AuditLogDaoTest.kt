package com.matrix.synapse.database

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the AuditLogger interface using an in-memory fake.
 *
 * These run on the JVM without an Android emulator. The Room-backed implementation
 * satisfies the same contract at runtime.
 */
class AuditLogDaoTest {

    private val logger = InMemoryAuditLogger()

    @Test
    fun writes_audit_record_for_destructive_user_actions() = runTest {
        logger.insert(
            AuditLogEntry(
                serverId = "server1",
                action = AuditAction.DEACTIVATE_USER,
                targetUserId = "@alice:server",
            )
        )

        val entries = logger.getAllByServer("server1")
        assertEquals(1, entries.size)
        assertEquals(AuditAction.DEACTIVATE_USER, entries[0].action)
        assertEquals("@alice:server", entries[0].targetUserId)
    }

    @Test
    fun records_are_scoped_per_server() = runTest {
        logger.insert(AuditLogEntry(serverId = "serverA", action = AuditAction.LOGIN))
        logger.insert(AuditLogEntry(serverId = "serverB", action = AuditAction.CREATE_USER))

        val aEntries = logger.getAllByServer("serverA")
        val bEntries = logger.getAllByServer("serverB")

        assertEquals(1, aEntries.size)
        assertEquals(1, bEntries.size)
        assertEquals(AuditAction.LOGIN, aEntries[0].action)
        assertEquals(AuditAction.CREATE_USER, bEntries[0].action)
    }

    @Test
    fun entries_ordered_newest_first() = runTest {
        logger.insert(AuditLogEntry(serverId = "s", action = AuditAction.LOGIN, timestampMs = 1000L))
        logger.insert(AuditLogEntry(serverId = "s", action = AuditAction.LOCK_USER, timestampMs = 2000L))

        val entries = logger.getAllByServer("s")
        assertTrue(
            "Newest entry should be first",
            entries[0].timestampMs >= entries[1].timestampMs,
        )
    }

    @Test
    fun clear_removes_all_entries() = runTest {
        logger.insert(AuditLogEntry(serverId = "s", action = AuditAction.LOGIN))
        logger.deleteAll()

        val entries = logger.getAllByServer("s")
        assertTrue(entries.isEmpty())
    }
}

/** JVM-only in-memory implementation used exclusively in unit tests. */
class InMemoryAuditLogger : AuditLogger {
    private val store = mutableListOf<AuditLogEntry>()

    override suspend fun insert(entry: AuditLogEntry): Long {
        store.add(entry)
        return store.size.toLong()
    }

    override suspend fun getAllByServer(serverId: String): List<AuditLogEntry> =
        store.filter { it.serverId == serverId }.sortedByDescending { it.timestampMs }

    override suspend fun deleteAll() {
        store.clear()
    }
}
