package com.matrix.synapse.feature.media.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaListViewModelTest {
    private val mediaRepository = mockk<MediaRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = MediaListViewModel(mediaRepository, auditLogger)

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadRoomMedia populates media items`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } returns RoomMediaResponse(
            local = listOf("abc123", "def456"), remote = listOf("ghi789"),
        )
        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", filterUserId = null, filterRoomId = "!room:example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.mediaItems.size)
            assertTrue(state.mediaItems[0].isLocal)
            assertFalse(state.mediaItems[2].isLocal)
        }
    }

    @Test
    fun `bulkDeleteMedia reports count and logs audit`() = runTest {
        coEvery { mediaRepository.bulkDeleteMedia(any(), any(), any(), any()) } returns DeleteMediaResponse(deletedMedia = listOf("a", "b"), total = 2)
        val vm = createVm()
        vm.init("https://example.com", "srv1", null, null)
        vm.state.test {
            vm.bulkDeleteMedia(beforeTs = 1000L, sizeGt = null, keepProfiles = null)
            val state = expectMostRecentItem()
            assertEquals("Deleted 2 media items", state.actionMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.BULK_DELETE_MEDIA }) }
        }
    }

    @Test
    fun `purgeRemoteMediaCache reports count and logs audit`() = runTest {
        coEvery { mediaRepository.purgeRemoteMediaCache(any(), any()) } returns PurgeMediaCacheResponse(deleted = 5)
        val vm = createVm()
        vm.init("https://example.com", "srv1", null, null)
        vm.state.test {
            vm.purgeRemoteMediaCache(beforeTs = 1000L)
            val state = expectMostRecentItem()
            assertEquals("Purged 5 remote media items", state.actionMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.PURGE_REMOTE_MEDIA_CACHE }) }
        }
    }

    @Test
    fun `loadRoomMedia sets error on failure`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } throws RuntimeException("network error")
        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", null, "!room:x")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }
}
