package com.matrix.synapse.feature.users.domain

import com.matrix.synapse.feature.users.data.UserRepository
import javax.inject.Inject

class DeactivateUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    /**
     * Deactivates a Synapse user.
     *
     * When [deleteMedia] is true, all user media is listed and deleted on a best-effort basis
     * before the account is deactivated with `erase=true`. Individual media deletion failures
     * are swallowed so the overall deactivation can still proceed.
     *
     * [confirmed] must be `true`; callers are responsible for obtaining typed confirmation
     * from the administrator before calling this function.
     */
    suspend fun deactivate(
        serverUrl: String,
        userId: String,
        deleteMedia: Boolean,
        confirmed: Boolean,
    ): Result<Unit> = runCatching {
        require(confirmed) { "Deactivation requires explicit administrator confirmation" }

        if (deleteMedia) {
            val serverName = extractServerName(serverUrl)
            val mediaList = userRepository.listUserMedia(serverUrl, userId)
            mediaList.media.forEach { item ->
                // Best-effort: ignore individual failures so deactivation always proceeds
                runCatching { userRepository.deleteMedia(serverUrl, serverName, item.mediaId) }
            }
        }

        userRepository.deactivateUser(serverUrl, userId, erase = deleteMedia)
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
}
