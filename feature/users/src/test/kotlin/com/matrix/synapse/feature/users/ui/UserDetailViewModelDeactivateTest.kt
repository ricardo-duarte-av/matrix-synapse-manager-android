package com.matrix.synapse.feature.users.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
import com.matrix.synapse.network.CapabilityService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelDeactivateTest {

    private val userRepository = mockk<UserRepository>()
    private val capabilityService = mockk<CapabilityService>()
    private val deactivateUserUseCase = mockk<DeactivateUserUseCase>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = UserDetailViewModel(
        userRepository, capabilityService, deactivateUserUseCase, auditLogger,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deactivateUser sets isDeactivated on success`() = runTest {
        coEvery {
            deactivateUserUseCase.deactivate(any(), any(), any(), any())
        } returns Result.success(Unit)

        val vm = createVm()
        vm.state.test {
            vm.deactivateUser("https://x", "srv1", "@user:x", deleteMedia = false)
            val state = expectMostRecentItem()
            assertTrue(state.isDeactivated)
            assertFalse(state.isDeactivating)
            assertEquals("User deactivated", state.successMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.DEACTIVATE_USER }) }
        }
    }

    @Test
    fun `deactivateUser sets error on failure`() = runTest {
        coEvery {
            deactivateUserUseCase.deactivate(any(), any(), any(), any())
        } returns Result.failure(RuntimeException("deactivation failed"))

        val vm = createVm()
        vm.state.test {
            vm.deactivateUser("https://x", "srv1", "@user:x", deleteMedia = true)
            val state = expectMostRecentItem()
            assertFalse(state.isDeactivated)
            assertFalse(state.isDeactivating)
            assertNotNull(state.error)
        }
    }
}
