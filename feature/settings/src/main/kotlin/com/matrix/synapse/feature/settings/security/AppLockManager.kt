package com.matrix.synapse.feature.settings.security

import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the optional biometric/PIN app lock.
 *
 * - [isLockEnabled] persists across process restarts (stored in SharedPreferences).
 * - [isLocked] is in-memory: the app starts unlocked and becomes locked on [lock],
 *   which is called from [onResume] in MainActivity when lock is enabled.
 * - [unlock] is called after a successful BiometricPrompt authentication callback.
 */
interface AppLockManager {
    val isLockEnabled: StateFlow<Boolean>
    val isLocked: StateFlow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
    fun lock()
    fun unlock()
}
