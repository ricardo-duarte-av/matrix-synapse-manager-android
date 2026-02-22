package com.matrix.synapse.feature.auth.data

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/_matrix/client/v3/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}
