package com.matrix.synapse.feature.users.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.core.ui.SynapseTopBar
import com.matrix.synapse.feature.users.data.UserSummary
import com.matrix.synapse.feature.users.data.mxcToDownloadUrl
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    serverId: String,
    serverUrl: String,
    onUserClick: (userId: String) -> Unit,
    onAddUser: () -> Unit = {},
    onAuditLog: () -> Unit = {},
    onSettings: () -> Unit = {},
    onServers: () -> Unit = {},
    onRooms: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onMedia: () -> Unit = {},
    onFederation: () -> Unit = {},
    viewModel: UserListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var deleteDialogErase by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    deleteDialogErase?.let { erase ->
        AlertDialog(
            onDismissRequest = { deleteDialogErase = null },
            title = { Text(if (erase) "Delete with media?" else "Deactivate users?") },
            text = {
                Text(
                    if (erase) "Permanently deactivate ${state.selectedUserIds.size} user(s) and erase their data including media. This cannot be undone."
                    else "Deactivate ${state.selectedUserIds.size} user(s). They will not be able to log in. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedUsers(erase = erase)
                        deleteDialogErase = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(if (erase) "Delete with media" else "Deactivate") }
            },
            dismissButton = { TextButton(onClick = { deleteDialogErase = null }) { Text("Cancel") } },
        )
    }

    val sortedUsers = remember(state.users, state.sortOrder) {
        if (state.sortOrder == "name_desc") {
            state.users.sortedByDescending { (it.displayName ?: it.userId).lowercase() }
        } else {
            state.users.sortedBy { (it.displayName ?: it.userId).lowercase() }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SynapseTopBar(
                title = when {
                    state.selectionMode -> "${state.selectedUserIds.size} selected"
                    else -> state.currentServer?.displayName ?: serverUrl
                },
                subtitle = when {
                    state.selectionMode -> null
                    state.totalUsers > 0L -> "$serverUrl • ${state.totalUsers} users"
                    else -> serverUrl
                },
                onTitleClick = if (state.selectionMode) null else onServers,
                actions = {
                    if (state.selectionMode) {
                        TextButton(
                            onClick = { deleteDialogErase = false },
                            enabled = !state.isDeleting,
                        ) { Text("Delete") }
                        TextButton(
                            onClick = { deleteDialogErase = true },
                            enabled = !state.isDeleting,
                        ) { Text("Delete with media") }
                        TextButton(onClick = { viewModel.exitSelectionMode() }) {
                            Text("Cancel")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(text = { Text("Servers") }, onClick = { onServers(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Media") }, onClick = { onMedia(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Federation") }, onClick = { onFederation(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Rooms") }, onClick = { onRooms(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Stats") }, onClick = { onDashboard(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Audit log") }, onClick = { onAuditLog(); menuExpanded = false })
                                DropdownMenuItem(text = { Text("Settings") }, onClick = { onSettings(); menuExpanded = false })
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.selectionMode) {
                FloatingActionButton(
                    onClick = onAddUser,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add user")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    viewModel.search(q)
                },
                label = { Text("Search users") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("user_search"),
            )
            UserSortDropdown(
                sortOrder = state.sortOrder,
                onSortChange = { viewModel.setSortOrder(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("user_sort"),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("user_list_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("user_list_error"),
                )

                else -> UserList(
                    serverUrl = serverUrl,
                    users = sortedUsers,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    selectionMode = state.selectionMode,
                    selectedUserIds = state.selectedUserIds,
                    onUserClick = onUserClick,
                    onLoadMore = { viewModel.loadNextPage() },
                    onUserLongPress = { viewModel.enterSelectionMode(it) },
                    onToggleUserSelection = { viewModel.toggleUserSelection(it) },
                    onSelectAll = { viewModel.selectAllUsers() },
                    onClearSelection = { viewModel.clearUserSelection() },
                    currentUserId = state.currentUserId,
                )
            }
        }
    }
}

@Composable
private fun UserSortDropdown(
    sortOrder: String,
    onSortChange: (asc: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (sortOrder == "name_asc") "Name (A→Z)" else "Name (Z→A)"
    Box(modifier = modifier.clickable { expanded = true }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Name (A→Z)") }, onClick = { onSortChange(true); expanded = false })
            DropdownMenuItem(text = { Text("Name (Z→A)") }, onClick = { onSortChange(false); expanded = false })
        }
    }
}

@Composable
private fun UserList(
    serverUrl: String,
    users: List<UserSummary>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    selectionMode: Boolean,
    selectedUserIds: Set<String>,
    onUserClick: (userId: String) -> Unit,
    onLoadMore: () -> Unit,
    onUserLongPress: (userId: String) -> Unit,
    onToggleUserSelection: (userId: String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    currentUserId: String? = null,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_list"),
    ) {
        if (selectionMode) {
            item(key = "select_all") {
                val selectableUsers = currentUserId?.let { id -> users.filter { it.userId != id } } ?: users
                val allSelectableSelected = selectableUsers.isNotEmpty() && selectableUsers.all { it.userId in selectedUserIds }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (allSelectableSelected) onClearSelection() else onSelectAll()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = allSelectableSelected,
                        onCheckedChange = { if (it) onSelectAll() else onClearSelection() },
                        modifier = Modifier.testTag("user_select_all"),
                    )
                    Text("Select all", style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
        }
        items(users, key = { it.userId }) { user ->
            val isCurrentUser = user.userId == currentUserId
            UserRow(
                serverUrl = serverUrl,
                user = user,
                selectionMode = selectionMode,
                selected = user.userId in selectedUserIds,
                isCurrentUser = isCurrentUser,
                onClick = { onUserClick(user.userId) },
                onLongPress = { if (!isCurrentUser) onUserLongPress(user.userId) },
                onToggleSelection = { if (!isCurrentUser) onToggleUserSelection(user.userId) },
            )
            HorizontalDivider()
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun UserRow(
    serverUrl: String,
    user: UserSummary,
    selectionMode: Boolean,
    selected: Boolean,
    isCurrentUser: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val avatarUrl = mxcToDownloadUrl(serverUrl, user.avatarUrl)
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
                        enabled = !isCurrentUser,
                        modifier = Modifier.testTag("user_checkbox_${user.userId}"),
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
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        headlineContent = { Text(user.userId) },
        supportingContent = user.displayName?.let { { Text(it) } },
        modifier = Modifier
            .pointerInput(selectionMode, user.userId, isCurrentUser) {
                detectTapGestures(
                    onTap = {
                        if (selectionMode && !isCurrentUser) onToggleSelection()
                        else if (!selectionMode) onClick()
                    },
                    onLongPress = { if (!isCurrentUser) onLongPress() },
                )
            }
            .testTag("user_row_${user.userId}"),
    )
}
