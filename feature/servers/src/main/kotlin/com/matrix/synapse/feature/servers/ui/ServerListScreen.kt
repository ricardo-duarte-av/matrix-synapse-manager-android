package com.matrix.synapse.feature.servers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.model.Server

private val ScreenPadding = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onEditServer: (serverId: String) -> Unit,
    onOpenLogin: (serverId: String, serverUrl: String) -> Unit,
    onOpenUserList: (serverId: String, serverUrl: String) -> Unit,
    viewModel: ServerListViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val deletingId by viewModel.deletingId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ServerListNavEvent.OpenLogin -> onOpenLogin(event.serverId, event.serverUrl)
                is ServerListNavEvent.OpenUserList -> onOpenUserList(event.serverId, event.serverUrl)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers", style = MaterialTheme.typography.titleLarge) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServer,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add server")
            }
        },
    ) { padding ->
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(ScreenPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "No servers yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Add a Synapse server to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(servers, key = { it.id }) { server ->
                    val isDeleting = deletingId == server.id
                    ListItem(
                        headlineContent = { Text(server.displayName) },
                        supportingContent = { Text(server.homeserverUrl, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else {
                                IconButton(
                                    onClick = { onEditServer(server.id) },
                                    modifier = Modifier.padding(4.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit server",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeServer(server.id) },
                                    modifier = Modifier.padding(4.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove server",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDeleting) { viewModel.onServerClick(server) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
