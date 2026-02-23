package com.matrix.synapse.feature.jobs.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface JobsApi {
    @GET("/_synapse/admin/v1/background_updates/status")
    suspend fun getStatus(): BackgroundUpdatesStatusResponse

    @GET("/_synapse/admin/v1/background_updates/enabled")
    suspend fun getEnabled(): EnabledResponse

    @POST("/_synapse/admin/v1/background_updates/enabled")
    suspend fun setEnabled(@Body body: EnabledRequest): EnabledResponse

    @POST("/_synapse/admin/v1/background_updates/start_job")
    suspend fun startJob(@Body body: StartJobRequest)
}
