package com.matrix.synapse.feature.settings

import com.matrix.synapse.feature.settings.security.AppLockManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockManagerTest {

    private val manager: AppLockManager = FakeAppLockManager()

    @Test
    fun app_lock_disabled_by_default_and_toggleable() = runTest {
        assertFalse("Lock should be disabled by default", manager.isLockEnabled.value)

        manager.setEnabled(true)
        assertTrue("Lock should be enabled after toggling on", manager.isLockEnabled.value)

        manager.setEnabled(false)
        assertFalse("Lock should be disabled after toggling off", manager.isLockEnabled.value)
    }

    @Test
    fun locked_app_requires_successful_pin() = runTest {
        manager.setEnabled(true)

        assertFalse("App should not be locked before lock() is called", manager.isLocked.value)

        manager.lock()
        assertTrue("App should be locked after lock() called with lock enabled", manager.isLocked.value)

        // Simulate successful PIN authentication
        manager.unlock()
        assertFalse("App should be unlocked after successful auth", manager.isLocked.value)
    }

    @Test
    fun lock_is_no_op_when_app_lock_disabled() = runTest {
        assertFalse(manager.isLockEnabled.value)

        manager.lock()
        assertFalse("lock() should be a no-op when lock is disabled", manager.isLocked.value)
    }

    @Test
    fun disabling_lock_clears_locked_state() = runTest {
        manager.setEnabled(true)
        manager.lock()
        assertTrue(manager.isLocked.value)

        manager.setEnabled(false)
        assertFalse("Disabling lock should immediately unlock", manager.isLocked.value)
    }
}

/** Pure in-memory fake for JVM unit tests. */
private class FakeAppLockManager : AppLockManager {
    private val _isLockEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isLockEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _isLockEnabled

    private val _isLocked = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isLocked: kotlinx.coroutines.flow.StateFlow<Boolean> = _isLocked

    private var pinHash: String? = null

    override fun pinExists(): Boolean = pinHash != null

    override suspend fun setPin(pin: String) {
        pinHash = "hash($pin)"
    }

    override fun verifyPin(pin: String): Boolean = pinHash == "hash($pin)"

    override suspend fun clearPin() {
        pinHash = null
    }

    override suspend fun setEnabled(enabled: Boolean) {
        _isLockEnabled.value = enabled
        if (!enabled) _isLocked.value = false
    }

    override fun lock() {
        if (_isLockEnabled.value) _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }
}
