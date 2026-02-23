package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
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
    var roomIdInput by remember { mutableStateOf(filterRoomId ?: "") }
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
                actions = {
                    TextButton(onClick = { showBulkDeleteDialog = true }) { Text("Bulk Delete") }
                    TextButton(onClick = { showPurgeDialog = true }) { Text("Purge Cache") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filterRoomId == null && filterUserId == null) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Room ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("media_room_input"),
                    )
                    Button(
                        onClick = { viewModel.loadRoomMedia(roomIdInput) },
                        enabled = roomIdInput.isNotBlank(),
                    ) { Text("Load") }
                }
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("media_list_loading")) }

                state.error != null && state.mediaItems.isEmpty() -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp).testTag("media_list_error"),
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
                            Text(
                                "No media found",
                                modifier = Modifier.padding(16.dp),
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
