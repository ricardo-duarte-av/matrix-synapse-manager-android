package com.matrix.synapse.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents a Matrix API error body: {"errcode":"M_FORBIDDEN","error":"..."} */
@Serializable
data class MatrixError(
    @SerialName("errcode") val errcode: String,
    @SerialName("error") val error: String? = null,
)
