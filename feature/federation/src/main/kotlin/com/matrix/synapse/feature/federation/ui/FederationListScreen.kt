package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.federation.data.FederationDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationListScreen(
    serverUrl: String,
    serverId: String,
    onDestinationClick: (destination: String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: FederationListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl) { viewModel.init(serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Federation (${state.totalDestinations})") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(modifier = Modifier.testTag("federation_list_loading")) }

            state.error != null -> Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp).testTag("federation_list_error"),
            )

            else -> {
                val listState = rememberLazyListState()

                LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
                    if (lastVisible >= state.destinations.size - 5 && state.hasMore && !state.isLoadingMore) {
                        viewModel.loadNextPage()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).testTag("federation_list"),
                ) {
                    items(state.destinations, key = { it.destination }) { dest ->
                        DestinationRow(dest = dest, onClick = { onDestinationClick(dest.destination) })
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationRow(dest: FederationDestination, onClick: () -> Unit) {
    val healthColor = when {
        dest.failureTs == null -> Color(0xFF4CAF50)  // green — healthy
        dest.retryInterval > 0 -> Color(0xFFFFC107)  // yellow — retrying
        else -> Color(0xFFF44336)                      // red — failing
    }

    ListItem(
        headlineContent = { Text(dest.destination) },
        supportingContent = {
            if (dest.failureTs != null) {
                Text("Retry interval: ${formatInterval(dest.retryInterval)}")
            } else {
                Text("Healthy")
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(12.dp),
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = healthColor)
                }
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("federation_row_${dest.destination}"),
    )
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
