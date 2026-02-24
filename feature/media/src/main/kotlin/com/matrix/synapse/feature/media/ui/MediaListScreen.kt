package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.matrix.synapse.core.ui.Spacing
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserSummary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    serverUrl: String,
    serverId: String,
    filterUserId: String? = null,
    filterRoomId: String? = null,
    onMediaClick: (serverName: String, mediaId: String) -> Unit,
    onServers: () -> Unit = {},
    onBack: (() -> Unit)? = {},
    viewModel: MediaListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl) { viewModel.init(serverUrl, serverId, filterUserId, filterRoomId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var userDropdownExpanded by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.currentServer?.displayName ?: serverUrl,
                subtitle = serverUrl,
                onTitleClick = onServers,
                onBack = onBack,
                titleCentered = true,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.TightSpacing),
                horizontalArrangement = Arrangement.spacedBy(Spacing.TightSpacing),
            ) {
                Button(onClick = { showBulkDeleteDialog = true }) { Text("Bulk Delete") }
                OutlinedButton(onClick = { showPurgeDialog = true }) { Text("Purge Cache") }
            }
            Row(
                modifier = Modifier.padding(horizontal = Spacing.ScreenPadding, vertical = Spacing.TightSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RoomDropdown(
                    rooms = state.rooms,
                    roomsLoading = state.roomsLoading,
                    selectedRoomId = state.selectedRoomId,
                    expanded = roomDropdownExpanded,
                    onExpandedChange = { roomDropdownExpanded = it; if (it) userDropdownExpanded = false },
                    onRoomSelected = { viewModel.selectRoom(it) },
                    modifier = Modifier.weight(1f),
                )
                UserDropdown(
                    users = state.users,
                    usersLoading = state.usersLoading,
                    selectedUserId = state.selectedUserId,
                    expanded = userDropdownExpanded,
                    onExpandedChange = { userDropdownExpanded = it; if (it) roomDropdownExpanded = false },
                    onUserSelected = { viewModel.selectUser(it) },
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("media_list_loading")) }

                state.error != null && state.mediaItems.isEmpty() -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(Spacing.ScreenPadding).testTag("media_list_error"),
                )

                else -> LazyColumn(modifier = Modifier.testTag("media_list")) {
                    items(state.mediaItems, key = { it.mediaId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.mediaId, maxLines = 1) },
                            supportingContent = {
                                Text(if (item.isLocal) "Local" else "Remote")
                            },
                            modifier = Modifier
                                .clickable { onMediaClick(item.origin, item.mediaId) }
                                .testTag("media_row_${item.mediaId}"),
                        )
                    }
                    if (state.mediaItems.isEmpty()) {
                        item {
                            val emptyMessage = when {
                                filterRoomId != null || filterUserId != null -> "No media found"
                                state.selectedRoomId == null && state.selectedUserId == null ->
                                    "Select a room or user above to list media."
                                else -> "No media found"
                            }
                            Text(
                                emptyMessage,
                                modifier = Modifier.padding(Spacing.ScreenPadding).testTag("media_list_empty"),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        BulkDeleteDialog(
            onDismiss = { showBulkDeleteDialog = false },
            onConfirm = { days, sizeGt, keepProfiles ->
                showBulkDeleteDialog = false
                val beforeTs = System.currentTimeMillis() - (days * 86_400_000L)
                viewModel.bulkDeleteMedia(beforeTs, sizeGt, keepProfiles)
            },
        )
    }

    if (showPurgeDialog) {
        PurgeRemoteCacheDialog(
            onDismiss = { showPurgeDialog = false },
            onConfirm = { days ->
                showPurgeDialog = false
                val beforeTs = System.currentTimeMillis() - (days * 86_400_000L)
                viewModel.purgeRemoteMediaCache(beforeTs)
            },
        )
    }
}

@Composable
private fun BulkDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: (days: Long, sizeGt: Long?, keepProfiles: Boolean) -> Unit,
) {
    var daysText by remember { mutableStateOf("30") }
    var sizeText by remember { mutableStateOf("") }
    var keepProfiles by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Delete Local Media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete local media not accessed within the specified period.")
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days old") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Minimum size (bytes, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = keepProfiles, onCheckedChange = { keepProfiles = it })
                    Text("Keep profile media")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toLongOrNull() ?: return@Button
                    val size = sizeText.toLongOrNull()
                    onConfirm(days, size, keepProfiles)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PurgeRemoteCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: (days: Long) -> Unit,
) {
    var daysText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Purge Remote Media Cache") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Purge cached remote media not accessed within the specified period.")
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days old") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { daysText.toLongOrNull()?.let { onConfirm(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Purge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomDropdown(
    rooms: List<RoomSummary>,
    roomsLoading: Boolean,
    selectedRoomId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRoomSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRoom = rooms.find { it.roomId == selectedRoomId }
    val label = when {
        selectedRoomId == null -> "Select room"
        selectedRoom != null -> (selectedRoom.name?.takeIf { it.isNotBlank() } ?: selectedRoom.roomId)
        else -> selectedRoomId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("media_room_dropdown"),
    ) {
        OutlinedTextField(
            value = if (roomsLoading && rooms.isEmpty()) "Loading…" else label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Room") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("Select room") },
                onClick = { onRoomSelected(null); onExpandedChange(false) },
            )
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name?.takeIf { it.isNotBlank() } ?: room.roomId, maxLines = 1) },
                    onClick = { onRoomSelected(room.roomId); onExpandedChange(false) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDropdown(
    users: List<UserSummary>,
    usersLoading: Boolean,
    selectedUserId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUserSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedUser = users.find { it.userId == selectedUserId }
    val label = when {
        selectedUserId == null -> "Select user"
        selectedUser != null -> (selectedUser.displayName?.takeIf { it.isNotBlank() } ?: selectedUser.userId)
        else -> selectedUserId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("media_user_dropdown"),
    ) {
        OutlinedTextField(
            value = if (usersLoading && users.isEmpty()) "Loading…" else label,
            onValueChange = {},
            readOnly = true,
            label = { Text("User") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("Select user") },
                onClick = { onUserSelected(null); onExpandedChange(false) },
            )
            users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.displayName?.takeIf { it.isNotBlank() } ?: user.userId, maxLines = 1) },
                    onClick = { onUserSelected(user.userId); onExpandedChange(false) },
                )
            }
        }
    }
}
