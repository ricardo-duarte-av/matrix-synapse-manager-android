package com.matrix.synapse.feature.servers.domain

import com.matrix.synapse.feature.servers.data.WellKnownApi
import com.matrix.synapse.model.Server
import com.matrix.synapse.network.RetrofitFactory
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

data class DiscoveredServer(
    val inputUrl: String,
    val homeserverUrl: String,
)

class DiscoverServerUseCase @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    /**
     * Resolves a homeserver URL from [inputUrl].
     *
     * 1. Normalises the URL (prepends https:// if no scheme).
     * 2. Attempts `GET /.well-known/matrix/client` and extracts `m.homeserver.base_url`.
     * 3. On HTTP 404 or missing well-known entry, falls back to the normalised [inputUrl].
     * 4. On network error (unreachable host), returns [Result.failure].
     */
    suspend fun discover(inputUrl: String): Result<DiscoveredServer> {
        val normalizedUrl = normalizeUrl(inputUrl)
        return try {
            val api = retrofitFactory.create<WellKnownApi>(normalizedUrl)
            val wellKnown = api.getWellKnown()
            val homeserverUrl = wellKnown.homeserver?.baseUrl?.trimEnd('/') ?: normalizedUrl
            Result.success(DiscoveredServer(inputUrl = normalizedUrl, homeserverUrl = homeserverUrl))
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.success(DiscoveredServer(inputUrl = normalizedUrl, homeserverUrl = normalizedUrl))
            } else {
                Result.failure(e)
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
