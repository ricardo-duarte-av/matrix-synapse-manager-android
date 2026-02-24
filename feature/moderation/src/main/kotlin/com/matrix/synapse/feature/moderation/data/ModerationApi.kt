package com.matrix.synapse.feature.moderation.data

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ModerationApi {
    @GET("/_synapse/admin/v1/event_reports")
    suspend fun listEventReports(
        @Query("from") from: Long = 0L,
        @Query("limit") limit: Int = 100,
        @Query("dir") dir: String = "b",
        @Query("room_id") roomId: String? = null,
        @Query("user_id") userId: String? = null,
    ): EventReportsListResponse

    @GET("/_synapse/admin/v1/event_reports/{report_id}")
    suspend fun getEventReport(@Path("report_id") reportId: Long): EventReportDetailResponse

    @DELETE("/_synapse/admin/v1/event_reports/{report_id}")
    suspend fun deleteEventReport(@Path("report_id") reportId: Long)
}
