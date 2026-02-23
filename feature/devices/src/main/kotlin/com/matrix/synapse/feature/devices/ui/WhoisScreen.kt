package com.matrix.synapse.feature.devices.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.devices.data.DeviceRepository
import com.matrix.synapse.feature.devices.data.WhoisConnection
import com.matrix.synapse.feature.devices.data.WhoisInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class WhoisState(
    val whois: WhoisInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WhoisViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WhoisState())
    val state: StateFlow<WhoisState> = _state.asStateFlow()

    fun load(serverUrl: String, userId: String) {
        _state.value = WhoisState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                deviceRepository.getWhois(serverUrl, userId)
            }.onSuccess { whois ->
                _state.value = WhoisState(whois = whois)
            }.onFailure { e ->
                _state.value = WhoisState(error = e.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisScreen(
    serverUrl: String,
    userId: String,
    viewModel: WhoisViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl, userId) { viewModel.load(serverUrl, userId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions", style = MaterialTheme.typography.titleLarge) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("whois_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp).testTag("whois_error"),
                )

                state.whois != null -> WhoisContent(state.whois!!)
            }
        }
    }
}

@Composable
private fun WhoisContent(whois: WhoisInfo) {
    val connections: List<Pair<String, WhoisConnection>> = whois.devices.flatMap { (deviceId, activity) ->
        activity.sessions.flatMap { session ->
            session.connections.map { conn -> deviceId to conn }
        }
    }

    if (connections.isEmpty()) {
        Text(
            text = "No active connections",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("whois_sessions"),
    ) {
        itemsIndexed(connections) { index, (deviceId, connection) ->
            ConnectionCard(
                deviceId = deviceId,
                connection = connection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .testTag("whois_connection_$index"),
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    deviceId: String,
    connection: WhoisConnection,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Device: $deviceId", style = MaterialTheme.typography.labelMedium)
            connection.ip?.let { Text("IP: $it") }
            connection.userAgent?.let { Text("Agent: $it", style = MaterialTheme.typography.bodySmall) }
            connection.lastSeen?.let {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
                Text("Last seen: $date", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
