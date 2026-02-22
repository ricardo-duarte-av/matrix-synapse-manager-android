package com.matrix.synapse.network

/** Supplies the current access token for the active server session. */
fun interface TokenProvider {
    fun currentToken(): String?
}
