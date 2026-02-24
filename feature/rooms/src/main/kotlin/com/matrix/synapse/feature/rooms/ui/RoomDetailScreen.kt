package com.matrix.synapse.feature.rooms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.feature.rooms.data.DeleteRoomRequest
import com.matrix.synapse.feature.rooms.data.mxcToDownloadUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    serverUrl: String,
    serverId: String,
    roomId: String,
    onBack: () -> Unit = {},
    onMedia: () -> Unit = {},
    viewModel: RoomDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(roomId) { viewModel.loadRoom(serverUrl, serverId, roomId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var joinUserId by remember { mutableStateOf("") }
    var membersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleteComplete) {
        if (state.deleteComplete) onBack()
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.room?.name ?: "Room Detail",
                onBack = onBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.room == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(24.dp),
            )

            state.room != null -> {
                val room = state.room!!
                val roomAvatarUrl = mxcToDownloadUrl(serverUrl, room.avatar)
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Full-width avatar at top
                    if (roomAvatarUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        ) {
                            AsyncImage(
                                model = roomAvatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    // Header card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(room.name ?: "Unnamed Room", style = MaterialTheme.typography.headlineSmall)
                                if (room.topic != null) Text(room.topic, style = MaterialTheme.typography.bodyMedium)
                                if (room.canonicalAlias != null) Text(room.canonicalAlias, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { clipboardManager.setText(AnnotatedString(room.roomId)) }) {
                                    Text("Copy Room ID: ${room.roomId}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Info grid
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Room Info", style = MaterialTheme.typography.titleMedium)
                                InfoRow("Version", room.version ?: "\u2014")
                                InfoRow("Creator", room.creator ?: "\u2014")
                                InfoRow("Join Rules", room.joinRules ?: "\u2014")
                                InfoRow("Guest Access", room.guestAccess ?: "\u2014")
                                InfoRow("History Visibility", room.historyVisibility ?: "\u2014")
                                InfoRow("Federation", if (room.federatable) "Yes" else "No")
                                InfoRow("Encryption", room.encryption ?: "None")
                                InfoRow("State Events", room.stateEvents.toString())
                                InfoRow("Members", "${room.joinedMembers} (${room.joinedLocalMembers} local)")
                            }
                        }
                    }

                    // Members section
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                TextButton(onClick = { membersExpanded = !membersExpanded }) {
                                    Text(
                                        if (membersExpanded) "Hide Members (${state.members.size})"
                                        else "Show Members (${state.members.size})"
                                    )
                                }
                            }
                        }
                    }
                    if (membersExpanded) {
                        items(state.members) { member ->
                            Text(
                                member,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    // Status messages
                    item {
                        if (state.error != null) {
                            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("room_detail_error"))
                        }
                        if (state.actionMessage != null) {
                            Text(state.actionMessage!!, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Action buttons
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Actions", style = MaterialTheme.typography.titleMedium)

                                Button(
                                    onClick = { viewModel.blockRoom(serverUrl, serverId, roomId, !state.isBlocked) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(if (state.isBlocked) "Unblock Room" else "Block Room") }

                                Button(
                                    onClick = { showDeleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    enabled = !state.isDeleting,
                                ) { Text(if (state.isDeleting) "Deleting..." else "Delete Room") }

                                Button(
                                    onClick = { viewModel.makeRoomAdmin(serverUrl, serverId, roomId, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Make Me Room Admin") }

                                Button(
                                    onClick = onMedia,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Room Media") }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = joinUserId,
                                        onValueChange = { joinUserId = it },
                                        label = { Text("User ID") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.joinUserToRoom(serverUrl, serverId, roomId, joinUserId)
                                            joinUserId = ""
                                        },
                                        enabled = joinUserId.isNotBlank(),
                                    ) { Text("Join") }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteRoomDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { purge, block, message ->
                showDeleteDialog = false
                viewModel.deleteRoom(serverUrl, serverId, roomId, DeleteRoomRequest(purge = purge, block = block, message = message))
            },
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

@Composable
private fun DeleteRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (purge: Boolean, block: Boolean, message: String?) -> Unit,
) {
    var purge by remember { mutableStateOf(true) }
    var block by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action cannot be undone.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = purge, onCheckedChange = { purge = it })
                    Text("Purge (remove all traces)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = block, onCheckedChange = { block = it })
                    Text("Block room")
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Reason (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(purge, block, message.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
