package com.matrix.synapse.feature.federation.data

import retrofit2.http.*

interface FederationAdminApi {
    @GET("/_synapse/admin/v1/federation/destinations")
    suspend fun listDestinations(
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order_by") orderBy: String? = null,
        @Query("dir") dir: String? = null,
    ): FederationDestinationsResponse

    @GET("/_synapse/admin/v1/federation/destinations/{destination}")
    suspend fun getDestination(@Path("destination") destination: String): FederationDestination

    @GET("/_synapse/admin/v1/federation/destinations/{destination}/rooms")
    suspend fun getDestinationRooms(
        @Path("destination") destination: String,
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("dir") dir: String? = null,
    ): DestinationRoomsResponse

    @POST("/_synapse/admin/v1/federation/destinations/{destination}/reset_connection")
    suspend fun resetConnection(@Path("destination") destination: String)
}
