package com.matrix.synapse.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds "Authorization: Bearer <token>" to every request when a token is available.
 * The [tokenProvider] is called per-request so token changes take effect immediately.
 */
class AuthHeaderInterceptor(
    private val tokenProvider: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
