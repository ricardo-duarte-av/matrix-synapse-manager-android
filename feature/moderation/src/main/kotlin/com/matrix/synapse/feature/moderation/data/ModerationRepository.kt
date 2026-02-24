package com.matrix.synapse.feature.moderation.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModerationRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): ModerationApi = retrofitFactory.create(serverUrl)

    suspend fun listEventReports(
        serverUrl: String,
        from: Long = 0L,
        limit: Int = 100,
        dir: String = "b",
        roomId: String? = null,
        userId: String? = null,
    ): EventReportsListResponse =
        api(serverUrl).listEventReports(from = from, limit = limit, dir = dir, roomId = roomId, userId = userId)

    suspend fun getEventReport(serverUrl: String, reportId: Long): EventReportDetailResponse =
        api(serverUrl).getEventReport(reportId)

    suspend fun deleteEventReport(serverUrl: String, reportId: Long) {
        api(serverUrl).deleteEventReport(reportId)
    }
}
