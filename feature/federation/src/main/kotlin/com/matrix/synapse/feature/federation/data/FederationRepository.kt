package com.matrix.synapse.feature.federation.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FederationRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): FederationAdminApi = retrofitFactory.create(serverUrl)

    suspend fun listDestinations(
        serverUrl: String, from: String? = null, limit: Int = 100, orderBy: String? = null, dir: String? = null,
    ): FederationDestinationsResponse = api(serverUrl).listDestinations(from = from, limit = limit, orderBy = orderBy, dir = dir)

    suspend fun getDestination(serverUrl: String, destination: String): FederationDestination =
        api(serverUrl).getDestination(destination)

    suspend fun getDestinationRooms(
        serverUrl: String, destination: String, from: String? = null, limit: Int = 100,
    ): DestinationRoomsResponse = api(serverUrl).getDestinationRooms(destination, from = from, limit = limit)

    suspend fun resetConnection(serverUrl: String, destination: String) =
        api(serverUrl).resetConnection(destination)
}
