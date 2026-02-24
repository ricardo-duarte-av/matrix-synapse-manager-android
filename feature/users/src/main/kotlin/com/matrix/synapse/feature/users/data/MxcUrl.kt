package com.matrix.synapse.feature.users.data

/**
 * Converts a Matrix mxc:// URL to an HTTP download URL for the given server base.
 * Format: mxc://serverName/mediaId -> baseUrl/_matrix/media/r0/download/serverName/mediaId
 */
fun mxcToDownloadUrl(serverBaseUrl: String, mxc: String?): String? {
    if (mxc.isNullOrBlank() || !mxc.startsWith("mxc://")) return null
    val rest = mxc.removePrefix("mxc://")
    val parts = rest.split("/", limit = 2)
    if (parts.size != 2) return null
    val (serverName, mediaId) = parts
    val base = serverBaseUrl.trimEnd('/')
    return "$base/_matrix/media/r0/download/$serverName/$mediaId"
}
