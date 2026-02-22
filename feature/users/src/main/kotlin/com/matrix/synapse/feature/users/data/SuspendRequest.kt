package com.matrix.synapse.feature.users.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuspendRequest(
    @SerialName("suspend") val suspend: Boolean,
)
