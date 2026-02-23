package com.matrix.synapse.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a Synapse server the user has configured.
 *
 * @param id             Stable random identifier (not the Matrix server_name).
 * @param displayName    Human-readable label shown in the server list.
 * @param inputUrl       URL the user typed when adding the server.
 * @param homeserverUrl  Resolved base URL (from well-known or same as [inputUrl]).
 */
@Serializable
data class Server(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val inputUrl: String,
    val homeserverUrl: String,
)
