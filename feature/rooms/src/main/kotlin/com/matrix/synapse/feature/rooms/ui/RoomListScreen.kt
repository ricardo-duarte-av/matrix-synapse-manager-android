package com.matrix.synapse.feature.rooms.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.rooms.data.RoomSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    serverUrl: String,
    serverId: String,
    onRoomClick: (roomId: String) -> Unit,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: RoomListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { onServers() }) {
                        Text(
                            text = state.currentServer?.displayName ?: serverUrl,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q -> searchQuery = q; viewModel.search(q) },
                label = { Text("Search rooms") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("room_search"),
            )
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("room_list_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag("room_list_error"),
                )

                else -> RoomList(
                    rooms = state.rooms,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    onRoomClick = onRoomClick,
                    onLoadMore = { viewModel.loadNextPage() },
                )
            }
        }
    }
}

@Composable
private fun RoomList(
    rooms: List<RoomSummary>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onRoomClick: (roomId: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisible >= rooms.size - 5 && hasMore && !isLoadingMore) onLoadMore()
    }

    LazyColumn(state = listState, modifier = Modifier.testTag("room_list")) {
        items(rooms, key = { it.roomId }) { room ->
            RoomRow(room = room, onClick = { onRoomClick(room.roomId) })
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun RoomRow(room: RoomSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(room.name ?: room.roomId) },
        supportingContent = {
            Text("${room.joinedMembers} members" + if (room.encryption != null) " \u2022 encrypted" else "")
        },
        trailingContent = {
            if (room.canonicalAlias != null) Text(room.canonicalAlias, style = MaterialTheme.typography.bodySmall)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("room_row_${room.roomId}"),
    )
}
