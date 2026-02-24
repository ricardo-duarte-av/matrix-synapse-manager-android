package com.matrix.synapse.feature.rooms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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
    var deleteDialogPurge by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    deleteDialogPurge?.let { purge ->
        AlertDialog(
            onDismissRequest = { deleteDialogPurge = null },
            title = { Text(if (purge) "Delete with media?" else "Delete rooms?") },
            text = {
                Text(
                    if (purge) "Permanently delete ${state.selectedRoomIds.size} room(s) and remove all traces including media. This cannot be undone."
                    else "Delete ${state.selectedRoomIds.size} room(s). This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedRooms(purge = purge)
                        deleteDialogPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(if (purge) "Delete with media" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteDialogPurge = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SynapseTopBar(
                title = when {
                    state.selectionMode -> "${state.selectedRoomIds.size} selected"
                    else -> state.currentServer?.displayName ?: serverUrl
                },
                subtitle = if (state.selectionMode) null else serverUrl,
                onTitleClick = if (state.selectionMode) null else onServers,
                onBack = when {
                    state.selectionMode -> { { viewModel.exitSelectionMode() } }
                    else -> onBack
                },
                actions = {
                    if (state.selectionMode) {
                        TextButton(
                            onClick = { deleteDialogPurge = false },
                            enabled = !state.isDeleting,
                        ) { Text("Delete") }
                        TextButton(
                            onClick = { deleteDialogPurge = true },
                            enabled = !state.isDeleting,
                        ) { Text("Delete with media") }
                        TextButton(onClick = { viewModel.exitSelectionMode() }) {
                            Text("Cancel")
                        }
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
            RoomSortDropdown(
                sortBy = state.sortBy,
                sortDir = state.sortDir,
                onSortChange = { orderBy, dir -> viewModel.setSort(orderBy, dir) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("room_sort"),
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
                    roomAvatarUrls = state.roomAvatarUrls,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    selectionMode = state.selectionMode,
                    selectedRoomIds = state.selectedRoomIds,
                    onRoomClick = onRoomClick,
                    onLoadMore = { viewModel.loadNextPage() },
                    onRoomLongPress = { viewModel.enterSelectionMode(it) },
                    onToggleRoomSelection = { viewModel.toggleRoomSelection(it) },
                    onSelectAll = { viewModel.selectAllRooms() },
                    onClearSelection = { viewModel.clearRoomSelection() },
                )
            }
        }
    }
}

@Composable
private fun RoomSortDropdown(
    sortBy: String,
    sortDir: String,
    onSortChange: (orderBy: String, dir: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = remember(sortBy, sortDir) {
        when (sortBy) {
            "name" -> if (sortDir == "f") "Name (A→Z)" else "Name (Z→A)"
            "joined_members" -> if (sortDir == "f") "Members (most first)" else "Members (least first)"
            "state_events" -> if (sortDir == "b") "State events (most first)" else "State events (least first)"
            else -> "Sort"
        }
    }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sort by",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                Triple("name", "f", "Name (A→Z)"),
                Triple("name", "b", "Name (Z→A)"),
                Triple("joined_members", "f", "Members (most first)"),
                Triple("joined_members", "b", "Members (least first)"),
                Triple("state_events", "b", "State events (most first)"),
                Triple("state_events", "f", "State events (least first)"),
            ).forEach { (orderBy, dir, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSortChange(orderBy, dir); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun RoomList(
    rooms: List<RoomSummary>,
    roomAvatarUrls: Map<String, String>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    selectionMode: Boolean,
    selectedRoomIds: Set<String>,
    onRoomClick: (roomId: String) -> Unit,
    onLoadMore: () -> Unit,
    onRoomLongPress: (roomId: String) -> Unit,
    onToggleRoomSelection: (roomId: String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    val listState = rememberLazyListState()
    val allLoadedSelected = rooms.isNotEmpty() && rooms.all { it.roomId in selectedRoomIds }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (lastVisible >= rooms.size - 5 && hasMore && !isLoadingMore) onLoadMore()
    }

    LazyColumn(state = listState, modifier = Modifier.testTag("room_list")) {
        if (selectionMode) {
            item(key = "select_all") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (allLoadedSelected) onClearSelection() else onSelectAll()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = allLoadedSelected,
                        onCheckedChange = { if (it) onSelectAll() else onClearSelection() },
                        modifier = Modifier.testTag("room_select_all"),
                    )
                    Text("Select all", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        items(rooms, key = { it.roomId }) { room ->
            RoomRow(
                room = room,
                avatarUrl = roomAvatarUrls[room.roomId],
                selectionMode = selectionMode,
                selected = room.roomId in selectedRoomIds,
                onClick = { onRoomClick(room.roomId) },
                onLongPress = { onRoomLongPress(room.roomId) },
                onToggleSelection = { onToggleRoomSelection(room.roomId) },
            )
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
private fun RoomRow(
    room: RoomSummary,
    avatarUrl: String?,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.testTag("room_checkbox_${room.roomId}"),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        headlineContent = { Text(room.name ?: room.roomId) },
        supportingContent = {
            Text("${room.joinedMembers} members" + if (room.encryption != null) " \u2022 encrypted" else "")
        },
        trailingContent = {
            if (room.canonicalAlias != null) Text(room.canonicalAlias, style = MaterialTheme.typography.bodySmall)
        },
        modifier = Modifier
            .pointerInput(selectionMode, room.roomId) {
                detectTapGestures(
                    onTap = { if (selectionMode) onToggleSelection() else onClick() },
                    onLongPress = { onLongPress() },
                )
            }
            .testTag("room_row_${room.roomId}"),
    )
}
