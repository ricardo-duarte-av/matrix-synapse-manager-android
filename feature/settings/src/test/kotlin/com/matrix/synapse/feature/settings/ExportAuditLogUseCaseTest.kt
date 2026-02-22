package com.matrix.synapse.feature.settings

import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.settings.domain.ExportAuditLogUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAuditLogUseCaseTest {

    private val logger = FakeAuditLogger()
    private val useCase = ExportAuditLogUseCase(logger)

    @Test
    fun exports_json_with_redacted_sensitive_fields() = runTest {
        logger.entries.add(
            AuditLogEntry(
                serverId = "server1",
                action = AuditAction.LOGIN,
                details = mapOf("access_token" to "secret_bearer_token", "user_id" to "@admin:server"),
            )
        )

        val json = useCase.exportAsJson("server1")

        assertFalse("access_token value should be redacted: $json", json.contains("secret_bearer_token"))
        assertTrue("user_id should be present: $json", json.contains("@admin:server"))
        assertTrue("Should be valid JSON array", json.trimStart().startsWith("["))
    }

    @Test
    fun export_includes_all_fields_except_sensitive() = runTest {
        logger.entries.add(
            AuditLogEntry(
                serverId = "s",
                action = AuditAction.DEACTIVATE_USER,
                targetUserId = "@alice:server",
                timestampMs = 1700000000000L,
            )
        )

        val json = useCase.exportAsJson("s")

        assertTrue("Should include action", json.contains("DEACTIVATE_USER"))
        assertTrue("Should include targetUserId", json.contains("@alice:server"))
        assertTrue("Should include timestampMs", json.contains("1700000000000"))
    }

    @Test
    fun export_returns_empty_array_when_no_entries() = runTest {
        val json = useCase.exportAsJson("empty_server")
        assertTrue("Should be empty JSON array: $json", json.trim() == "[]")
    }
}

class FakeAuditLogger : AuditLogger {
    val entries = mutableListOf<AuditLogEntry>()

    override suspend fun insert(entry: AuditLogEntry): Long {
        entries.add(entry)
        return entries.size.toLong()
    }

    override suspend fun getAllByServer(serverId: String): List<AuditLogEntry> =
        entries.filter { it.serverId == serverId }

    override suspend fun deleteAll() = entries.clear()
}
