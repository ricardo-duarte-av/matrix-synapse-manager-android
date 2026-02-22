package com.matrix.synapse.feature.settings.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppLockManager @Inject constructor(
    @ApplicationContext context: Context,
) : AppLockManager {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isLockEnabled.value = enabled
        if (!enabled) _isLocked.value = false
    }

    override fun lock() {
        if (_isLockEnabled.value) _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_ENABLED = "lock_enabled"
    }
}
