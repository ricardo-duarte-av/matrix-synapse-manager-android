package com.matrix.synapse.feature.users.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserAdminApi {

    @GET("/_synapse/admin/v2/users")
    suspend fun listUsers(
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("name") name: String? = null,
        @Query("guests") guests: Boolean? = null,
        @Query("deactivated") deactivated: Boolean? = null,
    ): UsersListResponse

    @GET("/_synapse/admin/v2/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserDetail

    @PUT("/_synapse/admin/v2/users/{userId}")
    suspend fun upsertUser(
        @Path("userId") userId: String,
        @Body request: UpsertUserRequest,
    ): UserDetail

    @PUT("/_synapse/admin/v1/suspend/{userId}")
    suspend fun setSuspended(
        @Path("userId") userId: String,
        @Body request: SuspendRequest,
    )
}
