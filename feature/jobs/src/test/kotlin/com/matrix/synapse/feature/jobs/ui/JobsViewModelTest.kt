package com.matrix.synapse.feature.jobs.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.jobs.data.BackgroundUpdatesStatusResponse
import com.matrix.synapse.feature.jobs.data.CurrentUpdateInfo
import com.matrix.synapse.feature.jobs.data.JobsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

    private val jobsRepository = mockk<JobsRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_populates_enabled_and_currentUpdates() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(
            enabled = true,
            currentUpdates = mapOf(
                "master" to CurrentUpdateInfo(
                    name = "populate_stats_process_rooms",
                    totalItemCount = 100L,
                    totalDurationMs = 5000.0,
                    averageItemsPerMs = 0.02,
                ),
            ),
        )

        val vm = JobsViewModel(jobsRepository)
        vm.state.test {
            vm.load("srv1", "https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(true, state.enabled)
            assertEquals(1, state.currentUpdates.size)
            assertEquals("populate_stats_process_rooms", state.currentUpdates["master"]?.name)
        }
    }

    @Test
    fun load_failure_sets_error() = runTest {
        coEvery { jobsRepository.getStatus(any()) } throws RuntimeException("network error")

        val vm = JobsViewModel(jobsRepository)
        vm.state.test {
            vm.load("srv1", "https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
        }
    }

    @Test
    fun setEnabled_success_updates_state() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { jobsRepository.setEnabled(any(), any()) } returns false

        val vm = JobsViewModel(jobsRepository)
        vm.load("srv1", "https://example.com")
        vm.state.test {
            vm.setEnabled(false)
            val state = expectMostRecentItem()
            assertFalse(state.isToggling)
            assertEquals(false, state.enabled)
        }
    }

    @Test
    fun startJob_success_refreshes_and_shows_message() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { jobsRepository.startJob(any(), any()) } returns Unit

        val vm = JobsViewModel(jobsRepository)
        vm.load("srv1", "https://example.com")
        vm.state.test {
            vm.startJob("regenerate_directory")
            val state = expectMostRecentItem()
            assertFalse(state.isStartingJob)
            assertTrue(state.successMessage?.contains("regenerate_directory") == true)
        }
    }
}
