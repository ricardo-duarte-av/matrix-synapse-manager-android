package com.matrix.synapse.feature.rooms.data

import retrofit2.http.*

interface RoomAdminApi {
    @GET("/_synapse/admin/v1/rooms")
    suspend fun listRooms(
        @Query("from") from: Int? = null,
        @Query("limit") limit: Int = 100,
        @Query("order_by") orderBy: String? = null,
        @Query("dir") dir: String? = null,
        @Query("search_term") searchTerm: String? = null,
    ): RoomListResponse

    @GET("/_synapse/admin/v1/rooms/{roomId}")
    suspend fun getRoom(@Path("roomId") roomId: String): RoomDetailResponse

    @GET("/_synapse/admin/v1/rooms/{roomId}/members")
    suspend fun getRoomMembers(@Path("roomId") roomId: String): RoomMembersResponse

    @HTTP(method = "DELETE", path = "/_synapse/admin/v2/rooms/{roomId}", hasBody = true)
    suspend fun deleteRoom(@Path("roomId") roomId: String, @Body request: DeleteRoomRequest): DeleteRoomResponse

    @GET("/_synapse/admin/v2/rooms/{roomId}/delete_status")
    suspend fun getDeleteStatus(@Path("roomId") roomId: String): DeleteStatusResponse

    @PUT("/_synapse/admin/v1/rooms/{roomId}/block")
    suspend fun blockRoom(@Path("roomId") roomId: String, @Body request: BlockRoomRequest): BlockRoomResponse

    @GET("/_synapse/admin/v1/rooms/{roomId}/block")
    suspend fun getBlockStatus(@Path("roomId") roomId: String): BlockRoomResponse

    @POST("/_synapse/admin/v1/join/{roomId}")
    suspend fun joinUserToRoom(@Path("roomId") roomId: String, @Body request: JoinRoomRequest)

    @POST("/_synapse/admin/v1/rooms/{roomId}/make_room_admin")
    suspend fun makeRoomAdmin(@Path("roomId") roomId: String, @Body request: MakeRoomAdminRequest)
}
