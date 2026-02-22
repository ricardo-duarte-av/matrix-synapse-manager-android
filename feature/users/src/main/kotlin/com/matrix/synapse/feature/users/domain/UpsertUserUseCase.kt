package com.matrix.synapse.feature.users.domain

import com.matrix.synapse.feature.users.data.UpsertUserRequest
import com.matrix.synapse.feature.users.data.UserDetail
import com.matrix.synapse.feature.users.data.UserRepository
import javax.inject.Inject

class UpsertUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    /**
     * Creates a new Synapse user. Requires a fully-qualified [userId] (e.g. `@alice:server`)
     * and a [password] of at least 8 characters.
     */
    suspend fun createUser(
        serverUrl: String,
        userId: String,
        password: String,
        displayName: String? = null,
        admin: Boolean = false,
    ): Result<UserDetail> = runCatching {
        require(userId.startsWith("@") && userId.contains(":")) {
            "Invalid user ID format. Expected @localpart:server"
        }
        require(password.length >= 8) { "Password must be at least 8 characters" }
        userRepository.upsertUser(
            serverUrl = serverUrl,
            userId = userId,
            request = UpsertUserRequest(
                password = password,
                displayName = displayName,
                admin = admin,
            ),
        )
    }

    /**
     * Updates fields on an existing user. Only non-null parameters are sent in the PUT body,
     * so omitted fields remain unchanged server-side.
     */
    suspend fun updateUser(
        serverUrl: String,
        userId: String,
        displayName: String? = null,
        admin: Boolean? = null,
    ): Result<UserDetail> = runCatching {
        userRepository.upsertUser(
            serverUrl = serverUrl,
            userId = userId,
            request = UpsertUserRequest(
                displayName = displayName,
                admin = admin,
            ),
        )
    }
}
