package com.matrix.synapse.feature.servers.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.matrix.synapse.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private val SERVERS_KEY = stringPreferencesKey("servers")
private val json = Json { ignoreUnknownKeys = true }

@Singleton
class ServerRepository @Inject constructor(
    @Named("servers_datastore") private val dataStore: DataStore<Preferences>,
) {

    val servers: Flow<List<Server>> = dataStore.data.map { prefs ->
        prefs[SERVERS_KEY]?.let { raw ->
            runCatching { Json.decodeFromString<List<Server>>(raw) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    fun getServerById(id: String): Flow<Server?> =
        servers.map { list -> list.firstOrNull { it.id == id } }

    suspend fun addServer(server: Server) {
        dataStore.edit { prefs ->
            val list = prefs[SERVERS_KEY]?.let { Json.decodeFromString<List<Server>>(it) } ?: emptyList()
            prefs[SERVERS_KEY] = Json.encodeToString<List<Server>>(list + server)
        }
    }

    suspend fun updateServer(server: Server) {
        dataStore.edit { prefs ->
            val list = prefs[SERVERS_KEY]?.let { Json.decodeFromString<List<Server>>(it) } ?: emptyList()
            prefs[SERVERS_KEY] = Json.encodeToString<List<Server>>(list.map { if (it.id == server.id) server else it })
        }
    }

    suspend fun removeServer(serverId: String) {
        dataStore.edit { prefs ->
            val list = prefs[SERVERS_KEY]?.let { Json.decodeFromString<List<Server>>(it) } ?: emptyList()
            prefs[SERVERS_KEY] = Json.encodeToString<List<Server>>(list.filter { it.id != serverId })
        }
    }
}
