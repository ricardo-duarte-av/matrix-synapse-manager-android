package com.matrix.synapse.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [SecureTokenStore] backed by [EncryptedSharedPreferences].
 * Uses Android Keystore via [MasterKey] so tokens are encrypted at rest.
 *
 * Passwords are never stored here — access tokens only.
 */
@Singleton
class TokenStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecureTokenStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_token_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // StateFlow to allow reactive observation even though SharedPreferences is synchronous.
    private val _state = MutableStateFlow(Unit)

    override fun accessTokenFlow(serverId: String): Flow<String?> =
        _state.map { prefs.getString(accessKey(serverId), null) }

    override suspend fun saveToken(serverId: String, accessToken: String) {
        prefs.edit().putString(accessKey(serverId), accessToken).apply()
        _state.value = Unit
    }

    override suspend fun saveUserId(serverId: String, userId: String) {
        prefs.edit().putString(userIdKey(serverId), userId).apply()
        _state.value = Unit
    }

    override fun currentUserIdFlow(serverId: String): Flow<String?> =
        _state.map { prefs.getString(userIdKey(serverId), null) }

    override suspend fun clearTokens(serverId: String) {
        prefs.edit()
            .remove(accessKey(serverId))
            .remove(userIdKey(serverId))
            .apply()
        _state.value = Unit
    }

    private fun accessKey(serverId: String) = "access_$serverId"
    private fun userIdKey(serverId: String) = "user_$serverId"
}
