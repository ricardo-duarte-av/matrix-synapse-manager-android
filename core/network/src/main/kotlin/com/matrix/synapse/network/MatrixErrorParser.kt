package com.matrix.synapse.network

import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safely parses Matrix error bodies from HTTP response strings.
 * Returns null on empty, invalid, or non-Matrix JSON rather than throwing.
 */
@Singleton
class MatrixErrorParser @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String?): MatrixError? {
        if (body.isNullOrBlank()) return null
        return try {
            val error = json.decodeFromString<MatrixError>(body)
            // Only return if errcode is actually present (required field)
            error
        } catch (_: Exception) {
            null
        }
    }
}
