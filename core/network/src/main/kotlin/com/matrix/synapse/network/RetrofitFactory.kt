package com.matrix.synapse.network

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject

/**
 * Creates per-server Retrofit instances sharing the same [OkHttpClient]
 * (so the [AuthHeaderInterceptor] and logging still apply) but with
 * distinct base URLs.
 */
class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun <T> create(baseUrl: String, service: Class<T>): T =
        Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(service)

    inline fun <reified T> create(baseUrl: String): T = create(baseUrl, T::class.java)

    private fun ensureTrailingSlash(url: String) =
        if (url.endsWith("/")) url else "$url/"
}
