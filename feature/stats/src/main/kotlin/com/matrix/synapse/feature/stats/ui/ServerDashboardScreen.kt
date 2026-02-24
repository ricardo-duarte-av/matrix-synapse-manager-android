package com.matrix.synapse.feature.stats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDashboardScreen(
    serverId: String,
    serverUrl: String,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: ServerDashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.loadDashboard(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.currentServer?.displayName ?: serverUrl,
                subtitle = serverUrl,
                onTitleClick = onServers,
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.serverVersion == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(24.dp),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadDashboard(serverId, serverUrl) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Version
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Server Version", style = MaterialTheme.typography.titleMedium)
                                Text(state.serverVersion ?: "\u2014", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }

                    // Summary row
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("Total Users", state.totalUsers.toString(), Modifier.weight(1f))
                            StatCard("Total Rooms", state.totalRooms.toString(), Modifier.weight(1f))
                        }
                    }

                    // Active users
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard("DAU (24h)", state.dau.toString(), Modifier.weight(1f))
                            StatCard("MAU (30d)", state.mau.toString(), Modifier.weight(1f))
                        }
                    }

                    // Total media storage
                    item {
                        StatCard(
                            label = "Total media storage",
                            value = state.totalMediaBytes?.let { formatBytes(it) } ?: "\u2014",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Largest rooms
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Largest Rooms (by DB size)", style = MaterialTheme.typography.titleMedium)
                            if (state.dbStatsUnavailable) {
                                Text(
                                    "PostgreSQL required \u2014 not available on SQLite",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (state.largestRooms.isEmpty()) {
                                Text("No data", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (!state.dbStatsUnavailable) {
                    items(state.largestRooms) { room ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(room.roomId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatBytes(room.estimatedSize), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Top media users
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top Media Users", style = MaterialTheme.typography.titleMedium)
                            if (state.topMediaUsers.isEmpty()) {
                                Text("No data", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(state.topMediaUsers) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayname ?: user.userId, style = MaterialTheme.typography.bodySmall)
                            if (user.displayname != null) Text(user.userId, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${user.mediaCount} files", style = MaterialTheme.typography.bodySmall)
                            Text(formatBytes(user.mediaLength), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
