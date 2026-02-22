package com.matrix.synapse.feature.servers.data

import retrofit2.http.GET

interface WellKnownApi {
    @GET("/.well-known/matrix/client")
    suspend fun getWellKnown(): WellKnownResponse
}
