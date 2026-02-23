package com.matrix.synapse.feature.media.data

import retrofit2.http.*

interface MediaAdminApi {
    @GET("/_synapse/admin/v1/room/{roomId}/media")
    suspend fun listRoomMedia(@Path("roomId") roomId: String): RoomMediaResponse

    @GET("/_synapse/admin/v1/media/{serverName}/{mediaId}")
    suspend fun getMediaInfo(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    ): MediaInfoResponse

    @POST("/_synapse/admin/v1/media/quarantine/{serverName}/{mediaId}")
    suspend fun quarantineMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    )

    @POST("/_synapse/admin/v1/media/unquarantine/{serverName}/{mediaId}")
    suspend fun unquarantineMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    )

    @POST("/_synapse/admin/v1/media/protect/{mediaId}")
    suspend fun protectMedia(@Path("mediaId") mediaId: String)

    @POST("/_synapse/admin/v1/media/unprotect/{mediaId}")
    suspend fun unprotectMedia(@Path("mediaId") mediaId: String)

    @DELETE("/_synapse/admin/v1/media/{serverName}/{mediaId}")
    suspend fun deleteMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    ): DeleteMediaResponse

    @POST("/_synapse/admin/v1/media/delete")
    suspend fun bulkDeleteMedia(
        @Query("before_ts") beforeTs: Long,
        @Query("size_gt") sizeGt: Long? = null,
        @Query("keep_profiles") keepProfiles: Boolean? = null,
    ): DeleteMediaResponse

    @POST("/_synapse/admin/v1/purge_media_cache")
    suspend fun purgeRemoteMediaCache(
        @Query("before_ts") beforeTs: Long,
    ): PurgeMediaCacheResponse
}
