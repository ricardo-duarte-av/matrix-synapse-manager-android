package com.matrix.synapse.feature.stats.data

import retrofit2.http.GET
import retrofit2.http.Query

interface StatsApi {
    @GET("/_synapse/admin/v1/server_version")
    suspend fun getServerVersion(): ServerVersionResponse

    @GET("/_synapse/admin/v1/statistics/database/rooms")
    suspend fun getDatabaseRoomStats(): DatabaseRoomStatsResponse

    @GET("/_synapse/admin/v1/statistics/users/media")
    suspend fun getMediaUsage(
        @Query("from") from: Int? = null,
        @Query("limit") limit: Int = 100,
        @Query("order_by") orderBy: String? = null,
    ): MediaUsageResponse
}
