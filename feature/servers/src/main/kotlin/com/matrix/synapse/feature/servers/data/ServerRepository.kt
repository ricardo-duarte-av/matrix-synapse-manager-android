package com.matrix.synapse.feature.servers.data

import com.matrix.synapse.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor() {

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: Flow<List<Server>> = _servers.asStateFlow()

    fun getServerById(id: String): Flow<Server?> =
        _servers.map { list -> list.firstOrNull { it.id == id } }

    suspend fun addServer(server: Server) {
        _servers.value = _servers.value + server
    }

    suspend fun updateServer(server: Server) {
        _servers.value = _servers.value.map { if (it.id == server.id) server else it }
    }

    suspend fun removeServer(serverId: String) {
        _servers.value = _servers.value.filter { it.id != serverId }
    }
}
