package com.matrix.synapse.feature.stats.data

import com.matrix.synapse.network.RetrofitFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

private interface UserStatsApi {
    @GET("/_synapse/admin/v2/users")
    suspend fun listUsers(
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 1,
    ): UserCountResponse
}

@Serializable
data class UserCountResponse(
    val users: List<UserTimestamp> = emptyList(),
    val total: Long = 0L,
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class UserTimestamp(
    @SerialName("name") val userId: String,
    @SerialName("last_seen_ts") val lastSeenTs: Long? = null,
)

private interface RoomCountApi {
    @GET("/_synapse/admin/v1/rooms")
    suspend fun listRooms(@Query("limit") limit: Int = 1): RoomCountResponse
}

@Serializable
data class RoomCountResponse(
    @SerialName("total_rooms") val totalRooms: Int = 0,
)

@Singleton
class StatsRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun statsApi(serverUrl: String): StatsApi = retrofitFactory.create(serverUrl)
    private fun userStatsApi(serverUrl: String): UserStatsApi = retrofitFactory.create(serverUrl)
    private fun roomCountApi(serverUrl: String): RoomCountApi = retrofitFactory.create(serverUrl)

    suspend fun getServerVersion(serverUrl: String): ServerVersionResponse =
        statsApi(serverUrl).getServerVersion()

    suspend fun getDatabaseRoomStats(serverUrl: String): DatabaseRoomStatsResponse =
        statsApi(serverUrl).getDatabaseRoomStats()

    suspend fun getMediaUsage(
        serverUrl: String,
        limit: Int = 100,
        orderBy: String? = null,
        dir: String? = null,
    ): MediaUsageResponse = statsApi(serverUrl).getMediaUsage(limit = limit, orderBy = orderBy, dir = dir)

    /** Sums media_length across all users (paginates through statistics/users/media). */
    suspend fun getTotalMediaStorage(serverUrl: String): Long {
        val api = statsApi(serverUrl)
        var total = 0L
        var from: Int? = null
        do {
            val response = api.getMediaUsage(from = from, limit = 100)
            total += response.users.sumOf { it.mediaLength }
            from = response.nextToken
        } while (from != null)
        return total
    }

    suspend fun getTotalUsers(serverUrl: String): Long =
        userStatsApi(serverUrl).listUsers(limit = 1).total

    suspend fun getTotalRooms(serverUrl: String): Int =
        roomCountApi(serverUrl).listRooms(limit = 1).totalRooms

    suspend fun getActiveUserCount(serverUrl: String, windowMs: Long): Int {
        val cutoff = System.currentTimeMillis() - windowMs
        var count = 0
        var from: String? = null
        val api = userStatsApi(serverUrl)
        do {
            val response = api.listUsers(from = from, limit = 500)
            count += response.users.count { (it.lastSeenTs ?: 0L) > cutoff }
            from = response.nextToken
        } while (from != null)
        return count
    }
}
