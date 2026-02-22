package com.matrix.synapse.feature.users.data

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for PUT /_synapse/admin/v2/users/{userId}.
 *
 * Nullable fields are annotated with [EncodeDefault.Mode.NEVER] so that
 * unset (null) properties are omitted from the JSON payload, avoiding
 * accidental overwrites of existing server-side values.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpsertUserRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("password") val password: String? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("displayname") val displayName: String? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("avatar_url") val avatarUrl: String? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("admin") val admin: Boolean? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("deactivated") val deactivated: Boolean? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("user_type") val userType: String? = null,

    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("locked") val locked: Boolean? = null,
)
