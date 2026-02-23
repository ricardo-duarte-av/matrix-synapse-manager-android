package com.matrix.synapse.manager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.servers.data.ServerRepository
import com.matrix.synapse.model.Server
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
) : ViewModel() {

    private val _currentServer = MutableStateFlow<Server?>(null)
    val currentServer: StateFlow<Server?> = _currentServer.asStateFlow()

    fun init(serverId: String) {
        serverRepository.getServerById(serverId).onEach { server ->
            _currentServer.value = server
        }.launchIn(viewModelScope)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    serverId: String,
    serverUrl: String,
    onFederation: () -> Unit,
    onBackgroundJobs: () -> Unit = {},
    onAuditLog: () -> Unit,
    onSettings: () -> Unit,
    onServers: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }
    val currentServer by viewModel.currentServer.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { onServers() }) {
                        Text(
                            text = currentServer?.displayName ?: serverUrl,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            ListItem(
                headlineContent = { Text("Federation") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFederation() },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Background jobs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackgroundJobs() },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Audit logs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuditLog() },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Settings") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSettings() },
            )
        }
    }
}
