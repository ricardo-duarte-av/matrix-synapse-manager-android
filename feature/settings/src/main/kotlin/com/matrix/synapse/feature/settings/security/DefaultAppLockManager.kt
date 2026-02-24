package com.matrix.synapse.feature.settings.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private const val PIN_LENGTH = 4
private const val PBKDF2_ITERATIONS = 10_000
private const val KEY_SALT = "pin_salt"
private const val KEY_HASH = "pin_hash"

@Singleton
class DefaultAppLockManager @Inject constructor(
    @ApplicationContext context: Context,
) : AppLockManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _isLockEnabled = MutableStateFlow(encryptedPrefs.getBoolean(KEY_ENABLED, false))
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    private val _isLocked = MutableStateFlow(_isLockEnabled.value)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun pinExists(): Boolean =
        encryptedPrefs.contains(KEY_HASH)

    override suspend fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH) { "PIN must be $PIN_LENGTH digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        encryptedPrefs.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    override fun verifyPin(pin: String): Boolean {
        if (pin.length != PIN_LENGTH) return false
        val saltB64 = encryptedPrefs.getString(KEY_SALT, null) ?: return false
        val storedHashB64 = encryptedPrefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.getDecoder().decode(saltB64)
        val storedHash = Base64.getDecoder().decode(storedHashB64)
        val computedHash = hashPin(pin, salt)
        return constantTimeEquals(storedHash, computedHash)
    }

    override suspend fun clearPin() {
        encryptedPrefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .apply()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isLockEnabled.value = enabled
        if (!enabled) _isLocked.value = false
    }

    override fun lock() {
        if (_isLockEnabled.value) _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            256,
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        return key.encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_ENABLED = "lock_enabled"
    }
}
