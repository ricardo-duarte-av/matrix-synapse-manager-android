package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationDetailScreen(
    serverUrl: String,
    serverId: String,
    destination: String,
    onRoomClick: ((roomId: String) -> Unit)? = null,
    onBack: () -> Unit = {},
    viewModel: FederationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(destination) { viewModel.loadDestination(serverUrl, serverId, destination) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = destination,
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.destination == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.destination == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(24.dp),
            )

            state.destination != null -> {
                val dest = state.destination!!
                val healthColor = when {
                    dest.failureTs == null -> Color(0xFF4CAF50)
                    dest.retryInterval > 0 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadDestination(serverUrl, serverId, destination) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Health header
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(modifier = Modifier.size(16.dp)) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(color = healthColor)
                                    }
                                }
                                Text(
                                    text = when {
                                        dest.failureTs == null -> stringResource(R.string.healthy)
                                        dest.retryInterval > 0 -> stringResource(R.string.retrying)
                                        else -> stringResource(R.string.failing)
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }

                    // Timing info
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.connection_info), style = MaterialTheme.typography.titleMedium)
                                InfoRow("First Failure", if (dest.failureTs != null) formatTimestamp(dest.failureTs) else "\u2014")
                                InfoRow("Last Retry", if (dest.retryLastTs > 0) formatTimestamp(dest.retryLastTs) else "\u2014")
                                InfoRow("Retry Interval", formatInterval(dest.retryInterval))
                            }
                        }
                    }

                    // Reset button
                    item {
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isResetting,
                        ) {
                            if (state.isResetting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Reset Connection")
                            }
                        }
                    }

                    // Shared rooms
                    item {
                        Text(stringResource(R.string.shared_rooms_count, state.totalRooms), style = MaterialTheme.typography.titleMedium)
                    }

                    items(state.rooms, key = { it.roomId }) { room ->
                        ListItem(
                            headlineContent = { Text(room.roomId, maxLines = 1) },
                            modifier = if (onRoomClick != null) {
                                Modifier.clickable { onRoomClick(room.roomId) }
                            } else {
                                Modifier
                            }.testTag("federation_room_${room.roomId}"),
                        )
                    }

                    if (state.hasMoreRooms) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMoreRooms(destination) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.load_more_rooms)) }
                        }
                    }
                }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_connection)) },
            text = { Text(stringResource(R.string.reset_connection_message, destination)) },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    viewModel.resetConnection(destination)
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "\u2014"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
